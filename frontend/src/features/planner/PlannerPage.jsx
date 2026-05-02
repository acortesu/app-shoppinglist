import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { isoDate, addDays, startOfWeekMonday, toPrettyDate } from '../../utils/helpers';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const PERIODS = ['WEEK', 'FORTNIGHT'];

export function PlannerPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [period, setPeriod] = useState('WEEK');
  const [startDate, setStartDate] = useState(isoDate(startOfWeekMonday()));
  const [recipes, setRecipes] = useState([]);
  const [plans, setPlans] = useState([]);
  const [slots, setSlots] = useState({});
  const [planId, setPlanId] = useState('');
  const [plannerNotice, setPlannerNotice] = useState('');
  const [plannerInlineError, setPlannerInlineError] = useState('');
  const [selectedDay, setSelectedDay] = useState('');
  const [pickerSlot, setPickerSlot] = useState(null);
  const [recipeQuery, setRecipeQuery] = useState('');

  const dayCount = period === 'WEEK' ? 7 : 14;
  const days = useMemo(
    () => Array.from({ length: dayCount }, (_, i) => isoDate(addDays(new Date(`${startDate}T00:00:00`), i))),
    [startDate, period]
  );

  const recipeNameById = useMemo(() => Object.fromEntries(recipes.map((r) => [r.id, r.name])), [recipes]);
  const validRecipeIds = useMemo(() => new Set(recipes.map((r) => r.id)), [recipes]);
  const selectedDayRecipes = useMemo(() => {
    const q = recipeQuery.trim().toLowerCase();
    if (!q) return recipes;
    return recipes.filter((r) => r.name.toLowerCase().includes(q));
  }, [recipes, recipeQuery]);

  const load = async () => {
    try {
      setBusy(true);
      const [recipeData, planData] = await Promise.all([api.listRecipes(), api.listPlans()]);
      setRecipes(recipeData || []);
      setPlans(planData || []);
    } catch (err) {
      notifyError(err, 'planner');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!isActive) return;
    load();
  }, [isActive]);

  useEffect(() => {
    const current = plans.find((p) => p.startDate === startDate && p.period === period);
    if (!current) {
      setPlanId('');
      setSlots({});
      setPlannerNotice('');
      return;
    }
    setPlanId(current.id);
    const map = {};
    let dropped = 0;
    (current.slots || []).forEach((s) => {
      if (validRecipeIds.has(s.recipeId)) {
        map[`${s.date}|${s.mealType}`] = s.recipeId;
      } else {
        dropped += 1;
      }
    });
    setSlots(map);
    setPlannerNotice(
      dropped > 0
        ? 'Se limpiaron slots con recetas eliminadas. Revisa y guarda el plan.'
        : ''
    );
  }, [plans, startDate, period, validRecipeIds]);

  useEffect(() => {
    if (!days.length) return;
    if (!selectedDay || !days.includes(selectedDay)) {
      setSelectedDay(days[0]);
    }
  }, [days, selectedDay]);

  const setSlot = (date, mealType, recipeId) => {
    const key = `${date}|${mealType}`;
    if (plannerInlineError) setPlannerInlineError('');
    setSlots((prev) => ({ ...prev, [key]: recipeId || '' }));
  };

  const clearSlot = (date, mealType) => {
    setSlot(date, mealType, '');
  };

  const savePlan = async () => {
    setPlannerInlineError('');
    const payloadSlots = [];
    days.forEach((d) => {
      MEAL_TYPES.forEach((mealType) => {
        const recipeId = slots[`${d}|${mealType}`];
        if (recipeId && validRecipeIds.has(recipeId)) {
          payloadSlots.push({ date: d, mealType, recipeId });
        }
      });
    });

    try {
      setBusy(true);
      const payload = { startDate, period, slots: payloadSlots };
      if (planId) {
        await api.updatePlan(planId, payload);
      } else {
        await api.createPlan(payload);
      }
      await load();
      notifySuccess('Plan guardado');
    } catch (err) {
      const msg = err?.payload?.error || err?.message || 'Error al guardar el plan';
      setPlannerInlineError(msg);
      notifyError(err, 'planner');
    } finally {
      setBusy(false);
    }
  };

  const goToToday = () => {
    const today = isoDate(new Date());
    setStartDate(isoDate(startOfWeekMonday()));
    setSelectedDay(today);
  };

  return (
    <section>
      <header className="top-header" style={{ '--screen-primary': 'var(--c4)' }}>
        <h1>Planificador</h1>
        <div className="row gap-sm">
          {PERIODS.map((p) => (
            <button
              key={p}
              className={`pill-btn ${period === p ? 'active' : ''}`}
              onClick={() => setPeriod(p)}
              style={period === p ? { '--screen-primary': 'var(--c4)' } : {}}
            >
              {p === 'WEEK' ? 'Semanal' : 'Quincenal'}
            </button>
          ))}
        </div>
        <div className="row between">
          <button
            className="btn"
            onClick={() => setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), -dayCount)))}
          >
            ←
          </button>
          <strong>{toPrettyDate(startDate)} - {toPrettyDate(days[days.length - 1])}</strong>
          <button
            className="btn"
            onClick={() => setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), dayCount)))}
          >
            →
          </button>
        </div>
        <button className="link-btn" onClick={goToToday}>Ir a hoy</button>
        {plannerNotice && <p className="notice-text">{plannerNotice}</p>}
        {plannerInlineError && <p className="field-error">{plannerInlineError}</p>}
      </header>

      <div className="stack">
        <div className="day-scroller">
          {days.map((date) => {
            const today = isoDate(new Date());
            const isToday = date === today;
            return (
              <button
                key={date}
                className={`day-chip ${selectedDay === date ? 'active' : ''}`}
                onClick={() => setSelectedDay(date)}
                style={isToday && selectedDay !== date ? { background: 'var(--c1)', color: '#ffffff', borderColor: 'transparent' } : {}}
              >
                {toPrettyDate(date)}
              </button>
            );
          })}
        </div>

        {selectedDay && (
          <article key={selectedDay} className="card">
            <h3 style={{ textTransform: 'capitalize', marginBottom: '12px' }}>{toPrettyDate(selectedDay)}</h3>
            {MEAL_TYPES.map((mealType) => {
              const key = `${selectedDay}|${mealType}`;
              const recipeId = slots[key] || '';
              const hasValidRecipe = recipeId && recipeNameById[recipeId];
              const mealLabel = { BREAKFAST: 'Desayuno', LUNCH: 'Almuerzo', DINNER: 'Cena' }[mealType];
              return (
                <div key={key} className="row between slot-row">
                  <div>
                    <div className="muted tiny">{mealLabel}</div>
                    <div style={{ fontWeight: 500 }}>{hasValidRecipe ? recipeNameById[recipeId] : 'Sin planificar'}</div>
                  </div>
                  <div className="row gap-sm">
                    <button
                      className="btn"
                      onClick={() => {
                        setPickerSlot({ date: selectedDay, mealType });
                        setRecipeQuery('');
                      }}
                      style={{ minWidth: 'auto', padding: '8px 12px' }}
                    >
                      {hasValidRecipe ? 'Cambiar' : '+'}
                    </button>
                    {hasValidRecipe && (
                      <button
                        className="btn"
                        onClick={() => clearSlot(selectedDay, mealType)}
                        style={{ minWidth: 'auto', padding: '8px 12px' }}
                      >
                        ×
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </article>
        )}
      </div>

      <button className="btn btn-primary sticky-cta" onClick={savePlan} style={{ '--screen-primary': 'var(--c4)' }}>
        Guardar plan
      </button>

      {pickerSlot && (
        <div className="modal-backdrop" onClick={(e) => {
          if (e.target === e.currentTarget) setPickerSlot(null);
        }}>
          <div className="modal">
            <div className="row between">
              <h2>Seleccionar receta</h2>
              <button type="button" className="link-btn" onClick={() => setPickerSlot(null)} style={{ fontSize: '20px' }}>✕</button>
            </div>
            <input
              className="input"
              placeholder="Buscar receta..."
              value={recipeQuery}
              onChange={(e) => setRecipeQuery(e.target.value)}
              autoFocus
            />
            <div className="stack no-pad">
              {selectedDayRecipes.map((r) => (
                <button
                  key={r.id}
                  className="btn"
                  onClick={() => {
                    setSlot(pickerSlot.date, pickerSlot.mealType, r.id);
                    setPickerSlot(null);
                  }}
                  style={{ textAlign: 'left' }}
                >
                  {r.name}
                </button>
              ))}
              {selectedDayRecipes.length === 0 && <p className="muted">No hay recetas para ese filtro.</p>}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

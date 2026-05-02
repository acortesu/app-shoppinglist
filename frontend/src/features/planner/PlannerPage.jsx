import { useEffect, useMemo, useRef, useState } from 'react';
import { api } from '../../api';
import { isoDate, addDays, startOfWeekMonday } from '../../utils/helpers';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const MEAL_LABEL = { BREAKFAST: 'Desayuno', LUNCH: 'Almuerzo', DINNER: 'Cena' };
const MEAL_ICONS = { BREAKFAST: '☀', LUNCH: '+', DINNER: '🌙' };

function getWeekNumber(isoDate) {
  const d = new Date(isoDate + 'T00:00:00');
  const startOfYear = new Date(d.getFullYear(), 0, 1);
  return Math.ceil(((d - startOfYear) / 86400000 + startOfYear.getDay() + 1) / 7);
}

function formatDateRange(startDate, dayCount) {
  const start = new Date(startDate + 'T00:00:00');
  const end = new Date(startDate + 'T00:00:00');
  end.setDate(end.getDate() + dayCount - 1);
  const month = start.toLocaleString('es-CR', { month: 'short' }).replace('.', '');
  return `${start.getDate()} — ${end.getDate()} ${month}`;
}

function getDayLabel(isoDate) {
  const today = new Date().toISOString().split('T')[0];
  const d = new Date(isoDate + 'T00:00:00');
  const weekday = d.toLocaleString('es-CR', { weekday: 'long' });
  if (isoDate === today) return `Hoy, ${weekday}`;
  return weekday.charAt(0).toUpperCase() + weekday.slice(1) + ', ' + d.getDate();
}

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
  const swipeStartX = useRef(null);

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

  const togglePeriod = () => {
    setPeriod((p) => (p === 'WEEK' ? 'FORTNIGHT' : 'WEEK'));
  };

  const goToToday = () => {
    const today = isoDate(new Date());
    setStartDate(isoDate(startOfWeekMonday()));
    setSelectedDay(today);
  };

  const goToPrevious = () => {
    setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), -dayCount)));
  };

  const goToNext = () => {
    setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), dayCount)));
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

  const onTouchStart = (e) => {
    swipeStartX.current = e.touches[0].clientX;
  };

  const onTouchEnd = (e) => {
    if (swipeStartX.current === null) return;
    const delta = e.changedTouches[0].clientX - swipeStartX.current;
    swipeStartX.current = null;
    if (Math.abs(delta) < 50) return;
    if (delta < 0) goToNext();
    else goToPrevious();
  };

  const mealPlannedCount = MEAL_TYPES.filter((mt) => slots[`${selectedDay}|${mt}`]).length;

  return (
    <section>
      <header className="planner-header">
        <div className="planner-header-top">
          <div>
            <p className="planner-week-label">SEMANA {getWeekNumber(startDate)}</p>
            <h1 className="planner-date-range">{formatDateRange(startDate, dayCount)}</h1>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
            <button className="planner-period-btn" onClick={togglePeriod} style={{ '--screen-primary': 'var(--c4)' }}>
              {period === 'WEEK' ? 'Semanal' : 'Quincenal'}
            </button>
            <button className="planner-today-btn" onClick={goToToday}>Hoy</button>
          </div>
        </div>

        {plannerNotice && <p className="notice-text">{plannerNotice}</p>}
        {plannerInlineError && <p className="field-error">{plannerInlineError}</p>}
      </header>

      <div className="planner-body">
        <div className="day-scroller" onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
          {days.map((date) => {
            const plannedMeals = MEAL_TYPES.filter((mt) => slots[`${date}|${mt}`]);
            return (
              <button
                key={date}
                className={`day-chip ${selectedDay === date ? 'active' : ''}`}
                onClick={() => setSelectedDay(date)}
              >
                <span className="day-chip-name">
                  {new Date(date + 'T00:00:00').toLocaleString('es-CR', { weekday: 'short' }).toUpperCase().replace('.', '')}
                </span>
                <span className="day-chip-number">
                  {new Date(date + 'T00:00:00').getDate()}
                </span>
                <div className="day-dots">
                  {MEAL_TYPES.map((mt) => (
                    <span
                      key={mt}
                      className={`dot ${plannedMeals.includes(mt) ? `dot-${mt.toLowerCase()}` : 'dot-empty'}`}
                    />
                  ))}
                </div>
              </button>
            );
          })}
        </div>

        <article className="planner-card">
          <div className="planner-card-header">
            <h3 className="planner-card-title">{getDayLabel(selectedDay)}</h3>
            <span className="planner-counter">{mealPlannedCount} / 3 planeado</span>
          </div>

          {MEAL_TYPES.map((mealType) => {
            const key = `${selectedDay}|${mealType}`;
            const recipeId = slots[key];
            return (
              <div key={mealType} className="planner-meal-row">
                <span className={`meal-icon meal-icon-${mealType.toLowerCase()}`}>
                  {MEAL_ICONS[mealType]}
                </span>
                <div className="meal-text">
                  <p className="meal-type-label">{MEAL_LABEL[mealType]}</p>
                  <p className="meal-name">{recipeId ? recipeNameById[recipeId] : 'Sin planificar'}</p>
                </div>
                <button
                  className={`meal-action-btn ${!recipeId ? 'meal-action-add' : ''}`}
                  onClick={() => {
                    setPickerSlot({ date: selectedDay, mealType });
                    setRecipeQuery('');
                  }}
                >
                  {recipeId ? 'Cambiar' : '+ Añadir'}
                </button>
              </div>
            );
          })}
        </article>

        <div style={{ padding: '0 18px 24px' }}>
          <button className="btn btn-primary planner-save-btn" onClick={savePlan} style={{ '--screen-primary': 'var(--c4)' }}>
            Guardar plan
          </button>
        </div>
      </div>

      {pickerSlot && (
        <div className="modal-backdrop" onClick={(e) => {
          if (e.target === e.currentTarget) {
            setPickerSlot(null);
            setRecipeQuery('');
          }
        }}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
              <h2 style={{ margin: 0 }}>Seleccionar receta</h2>
              <button
                type="button"
                className="link-btn"
                onClick={() => {
                  setPickerSlot(null);
                  setRecipeQuery('');
                }}
                style={{ fontSize: '20px' }}
              >
                ✕
              </button>
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
                    setRecipeQuery('');
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

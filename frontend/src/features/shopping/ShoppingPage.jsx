import { useEffect, useState } from 'react';
import { api } from '../../api';
import { toDisplayCase } from '../../utils/helpers';
import { ShoppingItem } from './ShoppingItem';

export function ShoppingPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [plans, setPlans] = useState([]);
  const [draft, setDraft] = useState(null);
  const [selectedPlanId, setSelectedPlanId] = useState('');
  const [validPlanIds, setValidPlanIds] = useState(new Set());
  const [shoppingNotice, setShoppingNotice] = useState('');
  const [showPlanPicker, setShowPlanPicker] = useState(false);

  const localizeDraft = (nextDraft, labelByIngredientId) => {
    if (!nextDraft?.items?.length) return nextDraft;
    return {
      ...nextDraft,
      items: nextDraft.items.map((item) => {
        if (item.manual || !item.ingredientId) {
          return { ...item, name: toDisplayCase(item.name) };
        }
        const preferred = labelByIngredientId[item.ingredientId];
        if (!preferred) return item;
        return { ...item, name: toDisplayCase(preferred) };
      })
    };
  };

  const load = async () => {
    try {
      setBusy(true);
      const [allPlans, allDrafts, allRecipes, allIngredients] = await Promise.all([
        api.listPlans(),
        api.listShoppingLists(),
        api.listRecipes(),
        api.listIngredients()
      ]);
      const recipeIds = new Set((allRecipes || []).map((r) => r.id));
      const labelByIngredientId = Object.fromEntries(
        (allIngredients || []).map((it) => [it.id, it.preferredLabel || it.name])
      );
      const nextValidPlanIds = new Set(
        (allPlans || [])
          .filter((p) => (p.slots || []).every((s) => recipeIds.has(s.recipeId)))
          .map((p) => p.id)
      );

      setPlans(allPlans || []);
      setValidPlanIds(nextValidPlanIds);

      const latestValidDraft = (allDrafts || []).find((d) => nextValidPlanIds.has(d.planId)) || null;
      const firstValidPlanId = (allPlans || []).find((p) => nextValidPlanIds.has(p.id))?.id || '';
      const nextSelectedPlanId = latestValidDraft?.planId || firstValidPlanId || '';

      setDraft(localizeDraft(latestValidDraft, labelByIngredientId));
      setSelectedPlanId(nextSelectedPlanId);
      setShoppingNotice(
        nextValidPlanIds.size < (allPlans || []).length
          ? 'Hay planes con recetas eliminadas. Solo se muestran planes válidos.'
          : ''
      );
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!isActive) return;
    load();
  }, [isActive]);

  const regenerate = async () => {
    if (!selectedPlanId) {
      notifyError(new Error('Selecciona un plan para generar lista'), 'shopping');
      return;
    }
    if (!validPlanIds.has(selectedPlanId)) {
      notifyError(new Error('El plan seleccionado tiene recetas eliminadas. Corrige el plan primero.'), 'shopping');
      return;
    }
    try {
      setBusy(true);
      const [created, allIngredients] = await Promise.all([
        api.generateShoppingList(selectedPlanId, `gen-${Date.now()}`),
        api.listIngredients()
      ]);
      const labelByIngredientId = Object.fromEntries(
        (allIngredients || []).map((it) => [it.id, it.preferredLabel || it.name])
      );
      setDraft(localizeDraft(created, labelByIngredientId));
      notifySuccess('Lista generada');
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  const updateItem = (id, patch) => {
    const normalizedPatch = patch && Object.prototype.hasOwnProperty.call(patch, 'name')
      ? { ...patch, name: toDisplayCase(patch.name) }
      : patch;
    setDraft((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        items: prev.items.map((it) => (it.id === id ? { ...it, ...normalizedPatch } : it))
      };
    });
  };

  const removeItem = (id) => {
    setDraft((prev) => {
      if (!prev) return prev;
      return { ...prev, items: prev.items.filter((it) => it.id !== id) };
    });
  };

  const addManual = () => {
    setDraft((prev) => {
      const base = prev || { id: '', planId: selectedPlanId || null, items: [] };
      const nextIndex = base.items.length;
      return {
        ...base,
        items: [
          ...base.items,
          {
            id: `tmp-${Date.now()}-${nextIndex}`,
            ingredientId: null,
            name: 'Nuevo Item',
            quantity: 1,
            unit: 'PIECE',
            suggestedPackages: null,
            packageAmount: null,
            packageUnit: null,
            manual: true,
            bought: false,
            note: '',
            sortOrder: nextIndex
          }
        ]
      };
    });
  };

  const saveDraft = async () => {
    if (!draft?.id) {
      notifyError(new Error('Primero genera una lista desde un plan'), 'shopping');
      return;
    }
    const items = (draft.items || []).map((it, idx) => ({
      id: it.id,
      ingredientId: it.ingredientId,
      name: it.name,
      quantity: Number(it.quantity),
      unit: it.unit,
      suggestedPackages: it.suggestedPackages,
      packageAmount: it.packageAmount,
      packageUnit: it.packageUnit,
      manual: Boolean(it.manual),
      bought: Boolean(it.bought),
      note: it.note || null,
      sortOrder: idx
    }));
    try {
      setBusy(true);
      const updated = await api.updateShoppingList(draft.id, items);
      setDraft(updated);
      notifySuccess('Cambios guardados');
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  const selectedPlan = plans.find((p) => p.id === selectedPlanId);
  const planDateLabel = selectedPlan
    ? new Date(selectedPlan.startDate + 'T00:00:00')
        .toLocaleString('es-CR', { day: 'numeric', month: 'short' })
        .toUpperCase()
        .replace('.', '')
    : null;

  const sortedItems = (draft?.items || []).slice().sort((a, b) => {
    if (a.bought !== b.bought) return a.bought ? 1 : -1;
    return a.sortOrder - b.sortOrder;
  });

  const boughtCount = (draft?.items || []).filter((it) => it.bought).length;
  const totalCount = (draft?.items || []).length;
  const pct = totalCount > 0 ? Math.round((boughtCount / totalCount) * 100) : 0;

  return (
    <section>
      <header className="shopping-header">
        <div className="shopping-header-top">
          {planDateLabel ? (
            <button className="shopping-plan-label" onClick={() => setShowPlanPicker((s) => !s)}>
              PLAN · {planDateLabel} ▾
            </button>
          ) : (
            <button className="shopping-plan-label" onClick={() => setShowPlanPicker((s) => !s)}>
              SIN PLAN ▾
            </button>
          )}
        </div>

        <h1 className="shopping-title">
          Lista de <span className="shopping-title-accent">compras</span>
        </h1>

        {showPlanPicker && (
          <div className="shopping-plan-picker">
            <select
              className="input"
              value={selectedPlanId}
              onChange={(e) => { setSelectedPlanId(e.target.value); setShowPlanPicker(false); }}
              autoFocus
            >
              <option value="">Selecciona un plan</option>
              {plans.filter((p) => validPlanIds.has(p.id)).map((p) => (
                <option key={p.id} value={p.id}>
                  {p.startDate} ({p.period === 'WEEK' ? 'Semanal' : 'Quincenal'})
                </option>
              ))}
            </select>
          </div>
        )}

        {shoppingNotice && <p className="notice-text">{shoppingNotice}</p>}

        {draft ? (
          <div className="shopping-progress-card">
            <div className="shopping-progress-top">
              <div>
                <p className="shopping-progress-label">{boughtCount} DE {totalCount} COMPRADOS</p>
                <p className="shopping-progress-pct">{pct}<span className="shopping-progress-pct-sym">%</span></p>
              </div>
              <button className="shopping-regen-btn" onClick={regenerate}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '6px' }}>
                  <polyline points="23 4 23 10 17 10"></polyline>
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                </svg>
                Regenerar
              </button>
            </div>
            <div className="shopping-progress-bar">
              <div className="shopping-progress-fill" style={{ width: `${pct}%` }} />
            </div>
          </div>
        ) : (
          <div className="shopping-empty-card">
            <p className="shopping-empty-text">Selecciona un plan y genera una lista.</p>
            <button className="shopping-regen-btn" onClick={regenerate}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '6px' }}>
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
              Generar lista
            </button>
          </div>
        )}
      </header>

      <div className="shopping-list">
        {sortedItems.map((item) => (
          <ShoppingItem
            key={item.id}
            item={item}
            onUpdate={updateItem}
            onRemove={removeItem}
          />
        ))}
        {draft && totalCount === 0 && (
          <p className="muted" style={{ padding: '24px 18px' }}>No hay items en la lista.</p>
        )}
      </div>

      {draft && (
        <div className="shopping-footer">
          <button className="shopping-add-btn" onClick={addManual}>
            + Agregar item
          </button>
          <button className="btn btn-primary shopping-save-btn" onClick={saveDraft} style={{ '--screen-primary': '#111111' }}>
            Guardar cambios
          </button>
        </div>
      )}
    </section>
  );
}

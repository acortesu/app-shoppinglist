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
  const [recipesById, setRecipesById] = useState({});
  const [draggingId, setDraggingId] = useState('');

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
      setRecipesById(Object.fromEntries((allRecipes || []).map((r) => [r.id, r])));
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
          ? 'Hay planes con recetas eliminadas. Solo se muestran planes válidos para generar lista.'
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

  const moveItem = (id, direction) => {
    setDraft((prev) => {
      if (!prev?.items?.length) return prev;
      const sorted = [...prev.items].sort((a, b) => a.sortOrder - b.sortOrder);
      const idx = sorted.findIndex((it) => it.id === id);
      if (idx < 0) return prev;
      const nextIdx = idx + direction;
      if (nextIdx < 0 || nextIdx >= sorted.length) return prev;
      const [moved] = sorted.splice(idx, 1);
      sorted.splice(nextIdx, 0, moved);
      return {
        ...prev,
        items: sorted.map((it, order) => ({ ...it, sortOrder: order }))
      };
    });
  };

  const moveItemByDrop = (sourceId, targetId) => {
    if (!sourceId || !targetId || sourceId === targetId) return;
    setDraft((prev) => {
      if (!prev?.items?.length) return prev;
      const sorted = [...prev.items].sort((a, b) => a.sortOrder - b.sortOrder);
      const sourceIdx = sorted.findIndex((it) => it.id === sourceId);
      const targetIdx = sorted.findIndex((it) => it.id === targetId);
      if (sourceIdx < 0 || targetIdx < 0) return prev;
      const [moved] = sorted.splice(sourceIdx, 1);
      sorted.splice(targetIdx, 0, moved);
      return {
        ...prev,
        items: sorted.map((it, order) => ({ ...it, sortOrder: order }))
      };
    });
  };

  const bulkSetBought = (value) => {
    setDraft((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        items: prev.items.map((it) => ({ ...it, bought: value }))
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

  const unbought = (draft?.items || []).filter((it) => !it.bought).sort((a, b) => a.sortOrder - b.sortOrder);
  const bought = (draft?.items || []).filter((it) => it.bought).sort((a, b) => a.sortOrder - b.sortOrder);
  const allBought = draft?.items?.length > 0 && draft.items.every((it) => it.bought);

  return (
    <section>
      <header className="top-header" style={{ '--screen-primary': 'var(--c2)' }}>
        <h1>Compras</h1>

        {draft && (
          <div className="card" style={{ background: 'var(--c2)', color: '#ffffff', marginBottom: '12px' }}>
            <div style={{ textAlign: 'center' }}>
              <div className="quantity-text" style={{ fontSize: '24px' }}>
                {unbought.length} DE {draft.items.length}
              </div>
              <div style={{ fontSize: '13px', opacity: 0.9, marginTop: '4px' }}>
                {draft.items.length > 0 ? `${Math.round((bought.length / draft.items.length) * 100)}%` : '0%'}
              </div>
              <div style={{ height: '4px', background: 'rgba(255,255,255,0.3)', borderRadius: '2px', marginTop: '8px', overflow: 'hidden' }}>
                <div style={{ height: '100%', background: '#ffffff', width: `${draft.items.length > 0 ? (bought.length / draft.items.length) * 100 : 0}%`, transition: 'width 0.2s' }} />
              </div>
            </div>
          </div>
        )}

        <select className="input" value={selectedPlanId} onChange={(e) => setSelectedPlanId(e.target.value)}>
          <option value="">Selecciona un plan</option>
          {plans.filter((p) => validPlanIds.has(p.id)).map((p) => (
            <option key={p.id} value={p.id}>
              {p.startDate} ({p.period === 'WEEK' ? 'Semanal' : 'Quincenal'})
            </option>
          ))}
        </select>

        <button className="btn btn-primary" onClick={regenerate} style={{ width: '100%', marginTop: '8px', '--screen-primary': 'var(--c2)' }}>
          {draft ? 'Regenerar lista' : 'Generar lista'}
        </button>

        {shoppingNotice && <p className="notice-text">{shoppingNotice}</p>}
      </header>

      <div className="stack">
        {draft ? (
          <>
            {unbought.length > 0 && (
              <div>
                <p className="section-title">POR COMPRAR</p>
                {unbought.map((item) => (
                  <ShoppingItem
                    key={item.id}
                    item={item}
                    onUpdate={updateItem}
                    onMove={moveItem}
                    onDrop={moveItemByDrop}
                    onRemove={removeItem}
                    draggingId={draggingId}
                    setDraggingId={setDraggingId}
                  />
                ))}
              </div>
            )}

            {bought.length > 0 && (
              <div>
                <p className="section-title">COMPRADO</p>
                {bought.map((item) => (
                  <ShoppingItem
                    key={item.id}
                    item={item}
                    onUpdate={updateItem}
                    onMove={moveItem}
                    onDrop={moveItemByDrop}
                    onRemove={removeItem}
                    draggingId={draggingId}
                    setDraggingId={setDraggingId}
                  />
                ))}
              </div>
            )}

            {draft.items.length === 0 && <p className="muted">No hay items en la lista.</p>}

            {draft.items.length > 0 && (
              <div className="row gap-sm">
                <button className="btn" onClick={() => bulkSetBought(false)} style={{ flex: 1 }}>
                  Marcar todo por comprar
                </button>
                <button className="btn" onClick={() => bulkSetBought(true)} style={{ flex: 1 }}>
                  Marcar todo comprado
                </button>
              </div>
            )}

            <button className="btn" onClick={addManual}>
              + Agregar item manual
            </button>

            <button className="btn btn-primary" onClick={saveDraft} style={{ '--screen-primary': 'var(--c2)' }}>
              Guardar cambios
            </button>
          </>
        ) : (
          <p className="muted">Selecciona un plan y genera una lista para comenzar.</p>
        )}
      </div>
    </section>
  );
}

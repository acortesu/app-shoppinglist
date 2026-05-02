import { useState } from 'react';
import { Modal } from '../../components/Modal';
import { api } from '../../api';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const MEASUREMENT_TYPES = ['WEIGHT', 'VOLUME', 'UNIT', 'TO_TASTE'];
const UNITS = ['GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON', 'PIECE', 'PINCH', 'TO_TASTE'];
const UNIT_LABELS_ES = {
  GRAM: 'gramo',
  KILOGRAM: 'kilogramo',
  MILLILITER: 'mililitro',
  LITER: 'litro',
  CUP: 'taza',
  TABLESPOON: 'cucharada',
  TEASPOON: 'cucharadita',
  PIECE: 'unidad (pieza/paquete)',
  PINCH: 'pizca',
  TO_TASTE: 'al gusto'
};
const MEASUREMENT_TYPE_LABELS_ES = {
  WEIGHT: 'Peso',
  VOLUME: 'Volumen',
  UNIT: 'Unidades',
  TO_TASTE: 'Al gusto'
};

function measurementTypeFromUnit(unit) {
  if (['GRAM', 'KILOGRAM'].includes(unit)) return 'WEIGHT';
  if (['MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON'].includes(unit)) return 'VOLUME';
  if (['PIECE'].includes(unit)) return 'UNIT';
  if (['PINCH', 'TO_TASTE'].includes(unit)) return 'TO_TASTE';
  return 'UNIT';
}

function unitsForMeasurementType(type) {
  if (type === 'WEIGHT') return ['GRAM', 'KILOGRAM'];
  if (type === 'VOLUME') return ['MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON'];
  if (type === 'UNIT') return ['PIECE'];
  if (type === 'TO_TASTE') return ['PINCH', 'TO_TASTE'];
  return UNITS;
}

function unitLabel(unit) {
  return UNIT_LABELS_ES[unit] || unit;
}

function ingredientLabel(opt) {
  return opt?.preferredLabel || opt?.name || '';
}

function ingredientMatchesQuery(name, query) {
  const normalizeText = (v) => String(v || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim();
  const normalizedQuery = normalizeText(query);
  const normalizedName = normalizeText(name);
  if (!normalizedQuery || !normalizedName) return false;
  if (normalizedName.startsWith(normalizedQuery)) return true;
  return normalizedName.split(/\s+/).some((token) => token.startsWith(normalizedQuery));
}

export function RecipeFormModal({ isOpen, initial, onClose, onSaved, setBusy, notifyError }) {
  const [name, setName] = useState(initial?.name || '');
  const [type, setType] = useState(initial?.type || 'LUNCH');
  const [preparation, setPreparation] = useState(initial?.preparation || '');
  const [notes, setNotes] = useState(initial?.notes || '');
  const [tags, setTags] = useState(initial?.tags?.join(', ') || '');
  const [ingredients, setIngredients] = useState(
    initial?.ingredients?.length
      ? initial.ingredients.map((it) => ({
          ingredientId: it.ingredientId || '',
          quantity: String(it.quantity ?? ''),
          unit: it.unit || 'GRAM',
          query: '',
          options: [],
          allowedUnits: null,
          customMeasurementType: 'UNIT'
        }))
      : [{ ingredientId: '', quantity: '', unit: 'PIECE', query: '', options: [], allowedUnits: null, customMeasurementType: 'UNIT' }]
  );
  const [formError, setFormError] = useState('');
  const [invalidIngredientIndexes, setInvalidIngredientIndexes] = useState([]);
  const [creatingCustomIdx, setCreatingCustomIdx] = useState(-1);

  const updateIngredient = (idx, patch) => {
    if (formError) setFormError('');
    if (invalidIngredientIndexes.length) setInvalidIngredientIndexes([]);
    setIngredients((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)));
  };

  const searchIngredients = async (idx, q) => {
    updateIngredient(idx, { query: q, ingredientId: '', allowedUnits: null });
    if (q.trim().length < 2) {
      updateIngredient(idx, { options: [] });
      return;
    }
    try {
      const options = await api.listIngredients(q.trim());
      const byNamePrefix = (options || []).filter((opt) => {
        if (ingredientMatchesQuery(ingredientLabel(opt), q)) return true;
        if (Array.isArray(opt.aliases)) {
          return opt.aliases.some((alias) => ingredientMatchesQuery(alias, q));
        }
        return ingredientMatchesQuery(opt.name, q);
      });
      const filtered = byNamePrefix.length > 0 ? byNamePrefix : (options || []);
      updateIngredient(idx, { options: filtered });
    } catch {
      updateIngredient(idx, { options: [] });
    }
  };

  const removeIngredient = (idx) => {
    if (ingredients.length <= 1) return;
    if (formError) setFormError('');
    if (invalidIngredientIndexes.length) setInvalidIngredientIndexes([]);
    setIngredients((prev) => prev.filter((_, i) => i !== idx));
  };

  const createCustomIngredientForRow = async (idx) => {
    const row = ingredients[idx];
    const name = (row?.query || '').trim();
    if (!name) {
      setFormError('Escribe el nombre del ingrediente antes de crear custom.');
      setInvalidIngredientIndexes([idx]);
      return;
    }

    try {
      setCreatingCustomIdx(idx);
      const measurementType = row.customMeasurementType || measurementTypeFromUnit(row.unit);
      const created = await api.createCustomIngredient({ name, measurementType });
      const allowedUnits = (created.allowedUnits || []).map(String);
      updateIngredient(idx, {
        ingredientId: created.id,
        query: created.name,
        allowedUnits,
        unit: allowedUnits.includes(row.unit) ? row.unit : (allowedUnits[0] || row.unit),
        customMeasurementType: measurementType,
        options: []
      });
      setFormError('');
      setInvalidIngredientIndexes([]);
    } catch (err) {
      const message = err?.payload?.error || err?.message || '';
      const existingMatch = message.match(/Ingredient already exists:\s*([^\s.]+)/i);
      const existingId = existingMatch?.[1]?.trim();
      if (existingId) {
        try {
          const [byName, byId] = await Promise.all([
            api.listIngredients(name),
            api.listIngredients(existingId)
          ]);
          const existing = [...(byName || []), ...(byId || [])].find((opt) => opt.id === existingId);
          if (existing) {
            const allowedUnits = (existing.allowedUnits || []).map(String);
            updateIngredient(idx, {
              ingredientId: existing.id,
              query: ingredientLabel(existing),
              allowedUnits,
              unit: allowedUnits.includes(row.unit) ? row.unit : (allowedUnits[0] || row.unit),
              options: []
            });
            setFormError('');
            setInvalidIngredientIndexes([]);
            return;
          }
        } catch {
          // fallback to default error handling
        }
      }
      notifyError(err, 'recipe_form');
    } finally {
      setCreatingCustomIdx(-1);
    }
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setInvalidIngredientIndexes([]);

    const normalizedIngredients = ingredients.map((it) => {
      const identifier = (it.ingredientId || it.query || '').trim();
      return {
        ingredientId: identifier,
        quantity: Number(it.quantity),
        unit: it.unit,
        allowedUnits: it.allowedUnits
      };
    });

    const missingIdx = normalizedIngredients
      .map((it, idx) => (!it.ingredientId || !Number.isFinite(it.quantity) || it.quantity <= 0 ? idx : -1))
      .filter((idx) => idx >= 0);

    if (missingIdx.length > 0) {
      setInvalidIngredientIndexes(missingIdx);
      setFormError('Revisa los ingredientes: nombre y cantidad son obligatorios.');
      return;
    }

    const invalidUnitIdx = normalizedIngredients
      .map((it, idx) => (Array.isArray(it.allowedUnits) && it.allowedUnits.length > 0 && !it.allowedUnits.includes(it.unit) ? idx : -1))
      .filter((idx) => idx >= 0);

    if (invalidUnitIdx.length > 0) {
      setInvalidIngredientIndexes(invalidUnitIdx);
      setFormError('La unidad seleccionada no está permitida para uno o más ingredientes.');
      return;
    }

    const payload = {
      name,
      type,
      preparation: preparation || null,
      notes: notes || null,
      tags: tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
      ingredients: normalizedIngredients.map(({ ingredientId, quantity, unit }) => ({ ingredientId, quantity, unit }))
    };

    try {
      setBusy(true);
      if (initial?.id) {
        await api.updateRecipe(initial.id, payload);
      } else {
        await api.createRecipe(payload);
      }
      await onSaved();
    } catch (err) {
      const backendMessage = err?.payload?.error || err?.message || '';
      if (err?.code === 'INGREDIENT_NOT_FOUND') {
        const match = backendMessage.match(/Unknown ingredient:\s*([^\.]+)/i);
        const unknown = match?.[1]?.trim().toLowerCase();
        const idx = ingredients.findIndex((it) => {
          const candidate = (it.ingredientId || it.query || '').trim().toLowerCase();
          return candidate && candidate === unknown;
        });
        setInvalidIngredientIndexes(idx >= 0 ? [idx] : []);
        setFormError('Ingrediente no encontrado. Selecciónalo desde catálogo o créalo como custom.');
      } else if (err?.code === 'INVALID_INGREDIENT_UNIT') {
        setFormError('La unidad no es válida para uno de los ingredientes seleccionados.');
      } else if (err?.code === 'VALIDATION_ERROR') {
        setFormError('Completa los campos obligatorios de la receta.');
      } else {
        notifyError(err, 'recipe_form');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <form onSubmit={onSubmit} className="stack">
        <div className="row between">
          <h2>{initial ? 'Editar Receta' : 'Nueva Receta'}</h2>
          <button type="button" className="link-btn" onClick={onClose} style={{ fontSize: '20px' }}>✕</button>
        </div>

        <div>
          <label>Nombre</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>

        <div>
          <label>Tipo</label>
          <select className="input" value={type} onChange={(e) => setType(e.target.value)}>
            {MEAL_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        <div>
          <label>Ingredientes</label>
          {ingredients.map((it, idx) => (
            <div key={idx} className={`ingredient-box ${invalidIngredientIndexes.includes(idx) ? 'ingredient-box-invalid' : ''}`}>
              <div className="row between">
                <span className="tiny muted">Ingrediente {idx + 1}</span>
                <button type="button" className="icon-btn danger" onClick={() => removeIngredient(idx)} disabled={ingredients.length <= 1}>✕</button>
              </div>
              <input
                className="input"
                placeholder="Nombre del ingrediente"
                value={it.query || it.ingredientId}
                onChange={(e) => searchIngredients(idx, e.target.value)}
              />
              {it.options?.length > 0 && (
                <div className="suggestions">
                  {it.options.slice(0, 6).map((opt) => (
                    <button
                      type="button"
                      key={opt.id}
                      className="suggestion"
                      onClick={() => {
                        const allowedUnits = (opt.allowedUnits || []).map(String);
                        const nextUnit = allowedUnits.includes(it.unit) ? it.unit : (allowedUnits[0] || 'GRAM');
                        updateIngredient(idx, {
                          ingredientId: opt.id,
                          query: ingredientLabel(opt),
                          unit: nextUnit,
                          allowedUnits,
                          options: []
                        });
                      }}
                    >
                      {ingredientLabel(opt)}
                    </button>
                  ))}
                </div>
              )}
              {it.query?.trim() && !it.ingredientId && (
                <button
                  type="button"
                  className="btn"
                  onClick={() => createCustomIngredientForRow(idx)}
                  disabled={creatingCustomIdx === idx}
                >
                  {creatingCustomIdx === idx ? 'Creando...' : `Crear "${it.query.trim()}" como custom`}
                </button>
              )}
              {!it.ingredientId && (
                <div className="row">
                  <select
                    className="input"
                    value={it.customMeasurementType || measurementTypeFromUnit(it.unit)}
                    onChange={(e) => {
                      const nextType = e.target.value;
                      const nextUnits = unitsForMeasurementType(nextType);
                      const nextUnit = nextUnits.includes(it.unit) ? it.unit : nextUnits[0];
                      updateIngredient(idx, { customMeasurementType: nextType, unit: nextUnit });
                    }}
                  >
                    {MEASUREMENT_TYPES.map((t) => <option key={t} value={t}>{MEASUREMENT_TYPE_LABELS_ES[t]}</option>)}
                  </select>
                </div>
              )}
              <div className="row">
                <input
                  className="input"
                  placeholder="Cantidad"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={it.quantity}
                  onChange={(e) => updateIngredient(idx, { quantity: e.target.value })}
                  required
                />
                <select
                  className="input"
                  value={it.unit}
                  onChange={(e) => {
                    const nextUnit = e.target.value;
                    updateIngredient(
                      idx,
                      it.ingredientId
                        ? { unit: nextUnit }
                        : { unit: nextUnit, customMeasurementType: measurementTypeFromUnit(nextUnit) }
                    );
                  }}
                >
                  {(Array.isArray(it.allowedUnits) && it.allowedUnits.length > 0
                    ? it.allowedUnits
                    : unitsForMeasurementType(it.customMeasurementType || measurementTypeFromUnit(it.unit))).map((u) => (
                    <option key={u} value={u}>{unitLabel(u)}</option>
                  ))}
                </select>
              </div>
            </div>
          ))}
          <button
            type="button"
            className="btn"
            onClick={() => setIngredients((prev) => [...prev, { ingredientId: '', quantity: '', unit: 'PIECE', query: '', options: [], allowedUnits: null, customMeasurementType: 'UNIT' }])}
          >
            + Agregar ingrediente
          </button>
        </div>

        {formError && <p className="auth-error">{formError}</p>}

        <div>
          <label>Instrucciones</label>
          <textarea className="input textarea" value={preparation} onChange={(e) => setPreparation(e.target.value)} />
        </div>

        <div>
          <label>Notas</label>
          <textarea className="input textarea" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>

        <div>
          <label>Tags (separados por coma)</label>
          <input className="input" value={tags} onChange={(e) => setTags(e.target.value)} />
        </div>

        <button className="btn btn-primary" type="submit">{initial ? 'Guardar cambios' : 'Crear receta'}</button>
      </form>
    </Modal>
  );
}

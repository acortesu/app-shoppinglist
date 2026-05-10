import { useState } from 'react';
import { api } from '../../api';

const MEASUREMENT_TYPES = ['WEIGHT', 'VOLUME', 'UNIT', 'TO_TASTE'];
const MEASUREMENT_TYPE_LABELS_ES = {
  WEIGHT: 'Peso',
  VOLUME: 'Volumen',
  UNIT: 'Unidades',
  TO_TASTE: 'Al gusto'
};
const CATEGORIES = ['Verduras y frutas', 'Lácteos', 'Proteínas', 'Granos', 'Condimentos', 'Bebidas'];

function unitsForMeasurementType(type) {
  if (type === 'WEIGHT') return ['GRAM', 'KILOGRAM'];
  if (type === 'VOLUME') return ['MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON'];
  if (type === 'UNIT') return ['PIECE'];
  if (type === 'TO_TASTE') return ['PINCH', 'TO_TASTE'];
  return [];
}

const UNIT_LABELS_ES = {
  GRAM: 'gramo (g)',
  KILOGRAM: 'kilogramo (kg)',
  MILLILITER: 'mililitro (ml)',
  LITER: 'litro (L)',
  CUP: 'taza',
  TABLESPOON: 'cucharada',
  TEASPOON: 'cucharadita',
  PIECE: 'unidad (pieza/paquete)',
  PINCH: 'pizca',
  TO_TASTE: 'al gusto'
};

export function IngredientModal({ isOpen, ingredientName, onClose, onCreated, notifyError }) {
  const [measurementType, setMeasurementType] = useState('UNIT');
  const [defaultUnit, setDefaultUnit] = useState('PIECE');
  const [category, setCategory] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleMeasurementTypeChange = (type) => {
    setMeasurementType(type);
    const units = unitsForMeasurementType(type);
    setDefaultUnit(units[0] || 'PIECE');
  };

  const handleCreate = async () => {
    try {
      setIsLoading(true);
      const payload = {
        name: ingredientName,
        measurementType
      };
      const created = await api.createCustomIngredient(payload);
      onCreated(created);
      setMeasurementType('UNIT');
      setDefaultUnit('PIECE');
      setCategory('');
    } catch (err) {
      notifyError(err, 'recipe_form');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="ingredient-modal-backdrop" onClick={onClose}>
      <div className="ingredient-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ingredient-modal-header">
          <button className="ingredient-modal-back" onClick={onClose}>‹</button>
          <h2 className="ingredient-modal-title">ingrediente</h2>
          <button className="ingredient-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="ingredient-modal-content">
          <p className="ingredient-modal-hint">
            «{ingredientName}» no está en el catálogo. Defínelo una vez y queda disponible para todas tus recetas y la lista de compras.
          </p>

          <div className="ingredient-modal-field">
            <label className="ingredient-modal-label">Nombre</label>
            <input
              className="ingredient-modal-input"
              type="text"
              value={ingredientName}
              disabled
            />
          </div>

          <div className="ingredient-modal-field">
            <label className="ingredient-modal-label">Tipo de unidad</label>
            <div className="ingredient-modal-chips">
              {MEASUREMENT_TYPES.map((type) => (
                <button
                  key={type}
                  className={`ingredient-modal-chip ${measurementType === type ? 'active' : ''}`}
                  onClick={() => handleMeasurementTypeChange(type)}
                >
                  {MEASUREMENT_TYPE_LABELS_ES[type]}
                </button>
              ))}
            </div>
            <p className="ingredient-modal-help">Define cómo se mide. La lista de compras suma solo ingredientes del mismo tipo.</p>
          </div>

          <div className="ingredient-modal-field">
            <label className="ingredient-modal-label">Unidad por defecto</label>
            <select
              className="ingredient-modal-input"
              value={defaultUnit}
              onChange={(e) => setDefaultUnit(e.target.value)}
            >
              {unitsForMeasurementType(measurementType).map((unit) => (
                <option key={unit} value={unit}>
                  {UNIT_LABELS_ES[unit]}
                </option>
              ))}
            </select>
          </div>

          <div className="ingredient-modal-field">
            <label className="ingredient-modal-label">Categoría (opcional)</label>
            <select
              className="ingredient-modal-input"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="">Sin categoría</option>
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
            <p className="ingredient-modal-help">Ayuda a agrupar la lista de compras por pasillo.</p>
          </div>
        </div>

        <div className="ingredient-modal-actions">
          <button className="ingredient-modal-btn-cancel" onClick={onClose}>
            Cancelar
          </button>
          <button
            className="ingredient-modal-btn-create"
            onClick={handleCreate}
            disabled={isLoading}
          >
            {isLoading ? 'Creando...' : 'Crear ingrediente'}
          </button>
        </div>
      </div>
    </div>
  );
}

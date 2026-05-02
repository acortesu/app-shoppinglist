import { useEffect, useState } from 'react';
import { api } from '../../api';

export function ShoppingPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [drafts, setDrafts] = useState([]);

  const loadDrafts = async () => {
    try {
      setBusy(true);
      const data = await api.listShoppingListDrafts();
      setDrafts(data || []);
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!isActive) return;
    loadDrafts();
  }, [isActive]);

  return (
    <section>
      <header className="top-header">
        <h1>Compras</h1>
      </header>

      <div className="stack">
        {drafts.length === 0 && <p className="muted">No hay listas de compra todavía.</p>}
        {drafts.map((draft) => (
          <div key={draft.id} className="card">
            <h3>Lista {draft.planId}</h3>
            <p className="tiny muted">{draft.items?.length || 0} items</p>
          </div>
        ))}
      </div>
    </section>
  );
}

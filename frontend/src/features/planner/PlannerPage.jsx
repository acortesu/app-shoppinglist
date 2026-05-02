import { useEffect, useState } from 'react';
import { api } from '../../api';

export function PlannerPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [plans, setPlans] = useState([]);

  const loadPlans = async () => {
    try {
      setBusy(true);
      const data = await api.listPlans();
      setPlans(data || []);
    } catch (err) {
      notifyError(err, 'planner');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!isActive) return;
    loadPlans();
  }, [isActive]);

  return (
    <section>
      <header className="top-header">
        <h1>Planificador</h1>
      </header>

      <div className="stack">
        {plans.length === 0 && <p className="muted">No hay planes todavía.</p>}
        {plans.map((plan) => (
          <div key={plan.id} className="card">
            <h3>{plan.startDate}</h3>
            <p className="tiny muted">{plan.period}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

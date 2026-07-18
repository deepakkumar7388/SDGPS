import React from 'react';

const PremiumInterProgressIndicator = ({ pass, onActivateExit }) => {
  if (!pass.destinationCampus) return null;

  // Derive current step from status
  const status = (pass.status || '').toLowerCase();
  
  const steps = [
    { key: 'approved', label: 'Approved', short: 'Appr' },
    { key: 'exited_source', label: 'Exited Source', short: 'Out Src' },
    { key: 'entered_destination', label: 'Entered Dest', short: 'In Dst' },
    { key: 'exited_destination', label: 'Exited Dest', short: 'Out Dst' },
    { key: 'entered_source', label: 'Entered Source', short: 'Done' }
  ];

  let currentStepIndex = -1;
  if (status === 'approved') currentStepIndex = 0;
  if (status === 'exited_source') currentStepIndex = 1;
  if (status === 'entered_destination') currentStepIndex = 2;
  if (status === 'exited_destination') currentStepIndex = 3;
  if (status === 'entered_source' || status === 'exit') currentStepIndex = 4;
  
  // If status is entered_source directly from exited_source without entered_destination, they bypassed
  const isBypassed = (status === 'entered_source' || status === 'exit') && pass.bypassedDestination;

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', background: 'var(--surface-card)', marginTop: '1rem' }}>
      <label style={{ fontSize: '0.7rem', color: 'var(--accent-primary)', textTransform: 'uppercase', letterSpacing: '0.08em', display: 'block', marginBottom: '1rem', fontWeight: 600 }}>
        Inter-Institutional Progress
      </label>
      
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'relative' }}>
        <div style={{ position: 'absolute', top: '15px', left: '10%', right: '10%', height: '3px', background: 'var(--glass-border)', zIndex: 0 }} />
        
        {currentStepIndex >= 0 && (
          <div style={{ 
            position: 'absolute', top: '15px', left: '10%', 
            width: `${(currentStepIndex / (steps.length - 1)) * 80}%`, 
            height: '3px', background: 'var(--accent-primary)', zIndex: 0,
            transition: 'width 0.4s ease'
          }} />
        )}

        {steps.map((step, idx) => {
          const isCompleted = currentStepIndex >= idx;
          const isCurrent = currentStepIndex === idx;
          
          let bypassedStyle = {};
          if (isBypassed && (idx === 2 || idx === 3)) {
            bypassedStyle = { opacity: 0.3, textDecoration: 'line-through' };
          }

          return (
            <div key={step.key} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', zIndex: 1, ...bypassedStyle }}>
              <div style={{ 
                width: '32px', height: '32px', borderRadius: '50%', 
                background: isCompleted ? 'var(--accent-primary)' : 'var(--surface-hover)',
                border: `3px solid ${isCurrent ? 'var(--text-primary)' : isCompleted ? 'var(--accent-primary)' : 'var(--glass-border)'}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: isCompleted ? '#fff' : 'var(--text-secondary)',
                fontSize: '0.8rem', fontWeight: 'bold',
                boxShadow: isCurrent ? '0 0 10px rgba(59,130,246,0.5)' : 'none',
                transition: 'all 0.3s ease'
              }}>
                {isCompleted ? '✓' : (idx + 1)}
              </div>
              <div style={{ fontSize: '0.7rem', marginTop: '0.5rem', color: isCurrent ? 'var(--text-primary)' : 'var(--text-secondary)', fontWeight: isCurrent ? 600 : 400, textAlign: 'center' }}>
                {step.short}
              </div>
            </div>
          );
        })}
      </div>

      {status === 'entered_destination' && (
        <div style={{ marginTop: '1rem', textAlign: 'center' }}>
          <p style={{ fontSize: '0.85rem', color: 'var(--warning)', marginBottom: '0.5rem' }}>Pass is inactive while at destination.</p>
          <button onClick={() => onActivateExit(pass)} className="btn btn-primary" style={{ padding: '0.5rem 1rem', fontSize: '0.8rem' }}>
            Activate (Exit Destination)
          </button>
        </div>
      )}
    </div>
  );
};

export default PremiumInterProgressIndicator;

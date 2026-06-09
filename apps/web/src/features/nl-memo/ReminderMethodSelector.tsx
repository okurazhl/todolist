const METHODS = [
  { key: 'browser', icon: '🔔', label: '浏览器通知' },
  { key: 'sound', icon: '🔊', label: '声音提醒' },
  { key: 'inapp', icon: '💬', label: '站内消息' },
  { key: 'email', icon: '📧', label: '邮件通知' },
];

interface Props {
  selected: string[];
  onChange: (v: string[]) => void;
}

export function ReminderMethodSelector({ selected, onChange }: Props) {
  const toggle = (key: string) => {
    if (selected.includes(key)) {
      onChange(selected.filter((k) => k !== key));
    } else {
      onChange([...selected, key]);
    }
  };

  return (
    <div className="nl-card">
      <div className="nl-card-title">
        <span className="step-num">4.</span> 提醒方式
      </div>
      <div className="nl-reminder-hint">到达时间时，将通过以下方式提醒你</div>
      <div className="nl-methods">
        {METHODS.map((m) => (
          <div
            key={m.key}
            className={`nl-method-card${selected.includes(m.key) ? ' selected' : ''}`}
            onClick={() => toggle(m.key)}
            style={{ position: 'relative' }}
          >
            {selected.includes(m.key) && (
              <div className="nl-method-check">✓</div>
            )}
            <span className="method-icon">{m.icon}</span>
            <span className="method-label">{m.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

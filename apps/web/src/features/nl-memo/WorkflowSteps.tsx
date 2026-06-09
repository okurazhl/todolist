const STEPS = [
  { icon: '🎙️', title: '自然语言输入', desc: '输入想法或语音录入' },
  { icon: '📅', title: '时间与事项识别', desc: 'AI 自动提取时间和事项' },
  { icon: '📄', title: '生成备忘录', desc: '整理成结构化备忘录' },
  { icon: '🔔', title: '创建提醒任务', desc: '生成定时提醒任务' },
  { icon: '✅', title: '到时通知用户', desc: '通过选定方式发出提醒' },
];

export function WorkflowSteps() {
  return (
    <div className="nl-workflow">
      <h3>工作流程</h3>
      <div className="nl-steps">
        {STEPS.map((step, i) => (
          <div key={step.title} style={{ display: 'flex', alignItems: 'flex-start', flex: 1 }}>
            <div className="nl-step">
              <div className="nl-step-icon">{step.icon}</div>
              <div className="nl-step-title">{step.title}</div>
              <div className="nl-step-desc">{step.desc}</div>
            </div>
            {i < STEPS.length - 1 && (
              <span className="nl-step-arrow">→</span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

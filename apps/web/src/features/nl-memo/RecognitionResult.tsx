import type { ParsedResult } from './NaturalLanguageMemoPage';

function formatDisplay(datetimeIso: string): string {
  const d = new Date(datetimeIso);
  const year = d.getFullYear();
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const hour = String(d.getHours()).padStart(2, '0');
  const minute = String(d.getMinutes()).padStart(2, '0');
  return `${year}年${month}月${day}日 ${hour}:${minute}`;
}

function isToday(datetimeIso: string): boolean {
  const d = new Date(datetimeIso);
  const now = new Date();
  return d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate();
}

interface Props {
  result: ParsedResult;
  onUseSuggestion: () => void;
}

export function RecognitionResult({ result, onUseSuggestion }: Props) {
  return (
    <div className="nl-card">
      <div className="nl-card-title">
        <span className="step-num">2.</span> 识别结果（AI 解析）
      </div>

      {/* Time row */}
      <div className="nl-recognition-row">
        <div className="nl-rec-icon">🕐</div>
        <div className="nl-rec-info">
          <div className="nl-rec-label">识别到时间</div>
          <div className="nl-rec-value">{formatDisplay(result.datetime)}</div>
        </div>
        <span className="nl-rec-arrow">→</span>
        <span className="nl-rec-parsed">
          {formatDisplay(result.datetime)}
          {isToday(result.datetime) && '（今天）'}
        </span>
        <span className="nl-badge-ok">已识别</span>
      </div>

      {/* Event row */}
      <div className="nl-recognition-row">
        <div className="nl-rec-icon">📋</div>
        <div className="nl-rec-info">
          <div className="nl-rec-label">识别到的事项</div>
          <div className="nl-rec-value">{result.event}</div>
        </div>
        <span className="nl-badge-ok">已识别</span>
      </div>

      {/* Time conflict warning */}
      {result.isPast && result.suggestedTime && (
        <div className="nl-warning">
          <div className="nl-warning-header">
            ⚠️ 时间已过提示
          </div>
          <div className="nl-warning-text">
            你输入的时间已经过去，建议修改为：
          </div>
          <div className="nl-suggested-time">
            {result.suggestedLabel}（{formatDisplay(result.suggestedTime)}）
          </div>
          <button className="nl-btn-use-suggestion" onClick={onUseSuggestion}>
            使用建议时间
          </button>
        </div>
      )}
    </div>
  );
}

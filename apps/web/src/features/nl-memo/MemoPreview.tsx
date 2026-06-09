import { useState } from 'react';
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

function formatNow(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const hour = String(d.getHours()).padStart(2, '0');
  const minute = String(d.getMinutes()).padStart(2, '0');
  return `${year}年${month}月${day}日 ${hour}:${minute}`;
}

interface Props {
  result: ParsedResult;
  remindAt: string;
  onResultChange: (r: ParsedResult) => void;
  onRemindAtChange: (v: string) => void;
  onSave: () => void;
  saving: boolean;
  onCancel: () => void;
}

export function MemoPreview({ result, remindAt, onResultChange, onRemindAtChange, onSave, saving, onCancel }: Props) {
  const [editing, setEditing] = useState(false);

  return (
    <div className="nl-card">
      <div className="nl-card-title">
        <span className="step-num">3.</span> 生成的备忘录
      </div>

      <div className="nl-preview-card">
        {/* Header */}
        <div className="nl-preview-header">
          <div className="nl-preview-header-left">
            📅 备忘录预览
          </div>
          <button
            className="nl-btn-edit"
            onClick={() => setEditing(!editing)}
          >
            {editing ? '✅ 完成' : '✏️ 编辑'}
          </button>
        </div>

        {/* Fields */}
        <div className="nl-preview-fields">
          <div className="nl-field-row">
            <span className="nl-field-label">标题</span>
            {editing ? (
              <span className="nl-field-value">
                <input
                  value={result.title}
                  onChange={(e) => onResultChange({ ...result, title: e.target.value })}
                  maxLength={256}
                />
              </span>
            ) : (
              <span className="nl-field-value">{result.title}</span>
            )}
          </div>
          <div className="nl-field-row">
            <span className="nl-field-label">内容</span>
            {editing ? (
              <span className="nl-field-value">
                <input
                  value={result.event}
                  onChange={(e) => onResultChange({ ...result, event: e.target.value })}
                  maxLength={50000}
                />
              </span>
            ) : (
              <span className="nl-field-value">{result.event}</span>
            )}
          </div>
          <div className="nl-field-row">
            <span className="nl-field-label">提醒时间</span>
            {editing ? (
              <span className="nl-field-value">
                <input
                  type="datetime-local"
                  value={remindAt.slice(0, 16)}
                  onChange={(e) => {
                    const v = e.target.value;
                    if (v) onRemindAtChange(v + ':00+08:00');
                  }}
                />
              </span>
            ) : (
              <span className="nl-field-value">
                📅 {formatDisplay(remindAt)}
                {isToday(remindAt) && '（今天）'}
              </span>
            )}
          </div>
          <div className="nl-field-row">
            <span className="nl-field-label">状态</span>
            <span className="nl-badge-status">待提醒</span>
          </div>
          <div className="nl-field-row">
            <span className="nl-field-label">创建时间</span>
            <span className="nl-field-value">{formatNow()}</span>
          </div>
        </div>

        {/* Actions */}
        <div className="nl-preview-actions">
          <button className="btn-secondary" onClick={onCancel}>取消</button>
          <button className="nl-btn-save" onClick={onSave} disabled={saving}>
            {saving ? '保存中...' : '保存并创建提醒'}
          </button>
        </div>
      </div>
    </div>
  );
}

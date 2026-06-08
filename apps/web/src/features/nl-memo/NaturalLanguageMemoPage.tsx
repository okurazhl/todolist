import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { InputSection } from './InputSection';
import { RecognitionResult } from './RecognitionResult';
import { MemoPreview } from './MemoPreview';
import { ReminderMethodSelector } from './ReminderMethodSelector';
import { WorkflowSteps } from './WorkflowSteps';
import { parseNaturalLanguage } from '../../shared/api/ai';
import { createMemo } from '../../shared/api/memos';

export interface ParsedResult {
  title: string;
  event: string;
  datetime: string;      // ISO 8601
  isPast: boolean;
  suggestedTime: string | null;
  suggestedLabel: string | null;
}

export function NaturalLanguageMemoPage() {
  const navigate = useNavigate();

  const [inputText, setInputText] = useState('');
  const [parsing, setParsing] = useState(false);
  const [parsedResult, setParsedResult] = useState<ParsedResult | null>(null);
  const [remindAt, setRemindAt] = useState('');
  const [reminderMethods, setReminderMethods] = useState<string[]>(['browser']);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleConvert = async () => {
    if (!inputText.trim()) return;
    setError('');
    setParsing(true);
    try {
      const result = await parseNaturalLanguage(inputText.trim());
      setParsedResult(result);
      setRemindAt(result.isPast && result.suggestedTime ? result.suggestedTime : result.datetime);
    } catch {
      setError('AI 解析失败，请重试');
    } finally {
      setParsing(false);
    }
  };

  const handleUseSuggestion = () => {
    if (parsedResult?.suggestedTime) {
      setParsedResult({
        ...parsedResult,
        datetime: parsedResult.suggestedTime,
        isPast: false,
      });
      setRemindAt(parsedResult.suggestedTime);
    }
  };

  const handleSave = async () => {
    if (!parsedResult) return;
    setSaving(true);
    setError('');
    try {
      const memo = await createMemo({
        title: parsedResult.title,
        content: parsedResult.event,
        remindAt: remindAt,
      });
      navigate(`/memo/${memo.id}`);
    } catch {
      setError('保存失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="nl-page">
      {/* Page header */}
      <div className="nl-page-header">
        <h2>自然语言创建备忘录</h2>
        <p className="nl-page-subtitle">输入你的想法，AI 帮你生成备忘录并设置提醒</p>
      </div>

      {/* Two-column layout */}
      <div className="nl-columns">
        {/* Left column: Input + Recognition */}
        <div className="nl-left-col">
          <InputSection
            value={inputText}
            onChange={setInputText}
            onConvert={handleConvert}
            converting={parsing}
          />

          {parsedResult && (
            <RecognitionResult
              result={parsedResult}
              onUseSuggestion={handleUseSuggestion}
            />
          )}
        </div>

        {/* Right column: Preview + Reminder methods */}
        <div className="nl-right-col">
          {parsedResult && (
            <MemoPreview
              result={parsedResult}
              remindAt={remindAt}
              onResultChange={setParsedResult}
              onRemindAtChange={setRemindAt}
              onSave={handleSave}
              saving={saving}
              onCancel={() => navigate(-1)}
            />
          )}

          {parsedResult && (
            <ReminderMethodSelector
              selected={reminderMethods}
              onChange={setReminderMethods}
            />
          )}
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* Bottom workflow */}
      <WorkflowSteps />
    </div>
  );
}

export function ErrorMessage({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="error-message">
      <p>{message}</p>
      {onRetry && <button onClick={onRetry}>重试</button>}
    </div>
  );
}

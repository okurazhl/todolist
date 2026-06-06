export function LoadingSpinner({ text = '加载中...' }: { text?: string }) {
  return <div className="loading-spinner">{text}</div>;
}

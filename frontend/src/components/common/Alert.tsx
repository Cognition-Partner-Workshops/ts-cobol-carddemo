interface AlertProps {
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  onClose?: () => void;
}

export default function Alert({ type, message, onClose }: AlertProps) {
  const typeClasses = {
    success: 'bg-green-100 border-green-500 text-green-700',
    error: 'bg-red-100 border-red-500 text-red-700',
    warning: 'bg-yellow-100 border-yellow-500 text-yellow-700',
    info: 'bg-blue-100 border-blue-500 text-blue-700',
  };

  return (
    <div className={`border-l-4 p-4 ${typeClasses[type]} mb-4`} role="alert">
      <div className="flex justify-between items-center">
        <p>{message}</p>
        {onClose && (
          <button onClick={onClose} className="text-lg font-bold ml-4">
            &times;
          </button>
        )}
      </div>
    </div>
  );
}

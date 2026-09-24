export type ToastVariant = "success" | "error" | "info";

export type ToastItem = {
  id: string;
  message: string;
  variant: ToastVariant;
  duration: number;
};

export type ToastOptions = {
  variant?: ToastVariant;
  duration?: number;
  id?: string;
};

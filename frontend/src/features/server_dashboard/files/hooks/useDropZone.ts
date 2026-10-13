import { useCallback, useState, type DragEvent } from "react";
import type { Payload } from "../types";
import { collectFromDrop } from "../utils/drop";

export function useDropZone(process: (payload: Payload) => Promise<void>) {
  const [dragging, setDragging] = useState<boolean>(false);

  const onDragOver = useCallback((e: DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "copy";
  }, []);

  const onDragEnter = useCallback((e: DragEvent) => {
    e.preventDefault();
    setDragging(true);
  }, []);

  const onDragLeaves = useCallback((e: DragEvent) => {
    if (!e.currentTarget.contains(e.relatedTarget as Node)) {
      setDragging(false);
    }
  }, []);

  const onDrop = useCallback(
    async (e: DragEvent) => {
      e.preventDefault();
      setDragging(false);

      const payload = await collectFromDrop(e.dataTransfer);
      if (payload) void process(payload);
    },
    [collectFromDrop, process],
  );

  return {
    dragging,
    onDragOver,
    onDragEnter,
    onDragLeaves,
    onDrop,
  };
}

import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { forwardRef, useEffect, useState } from "react";
import Cropper from "react-easy-crop";
import { Portal } from "../../components/Portal";
import SubmitButton from "../../components/SubmitButton";
import type { Crop } from "./ImageUtils";

interface ImageSelectionProps {
  isOpen: boolean;
  onClose: () => void;
  submitFunction: (imageUrl: string, crop: Crop) => void;
  isUpdatingPicture: boolean;
}

export const ImageSelection = forwardRef<HTMLInputElement, ImageSelectionProps>(function ImageSelection(
  { isOpen, isUpdatingPicture, onClose, submitFunction },
  ref,
) {
  const [img, setImg] = useState<string | null>(null);
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Crop | null>(null);

  useEffect(() => {
    return () => {
      if (img) URL.revokeObjectURL(img);
    };
  }, [img]);

  const handleSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (img) URL.revokeObjectURL(img);
    var fileUrl = URL.createObjectURL(file);
    setImg(fileUrl);

    e.target.value = "";
  };

  const handleClose = () => {
    if (img) URL.revokeObjectURL(img);

    setImg(null);
    setCroppedAreaPixels(null);
    onClose();
  };

  const handleSubmit = () => {
    if (img && croppedAreaPixels) {
      submitFunction(img, croppedAreaPixels);
    }
  };

  return (
    <>
      <input type="file" ref={ref} onChange={handleSelect} accept="image/*" style={{ display: "none" }} />
      <AnimatePresence>
        {isOpen && img && (
          <Portal>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="image-cropper-overlay"
            >
              <motion.div
                initial={{ y: -500, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                exit={{ y: -500, opacity: 0 }}
                className="image-cropper-container"
              >
                <div className="image-cropper-header">
                  <h4>Corte sua nova foto de perfil</h4>
                  <X className="image-cropper-close-btn" onClick={handleClose} />
                </div>
                <div className="image-cropper">
                  <Cropper
                    image={img}
                    crop={crop}
                    cropSize={{ height: 384, width: 384 }}
                    showGrid={false}
                    zoom={zoom}
                    onCropChange={setCrop}
                    onZoomChange={setZoom}
                    onCropComplete={(_croppedArea, pixels) => setCroppedAreaPixels(pixels)}
                  />
                </div>
                <div className="image-cropper-btns">
                  <SubmitButton
                    type="button"
                    label="Definir nova foto de perfil"
                    loading={isUpdatingPicture}
                    onClick={handleSubmit}
                    disabled={!croppedAreaPixels || isUpdatingPicture}
                  />
                </div>
              </motion.div>
            </motion.div>
          </Portal>
        )}
      </AnimatePresence>
    </>
  );
});

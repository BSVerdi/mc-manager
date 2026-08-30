import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { forwardRef, useState } from "react";
import Cropper from "react-easy-crop";
import { Portal } from "../features/create_server/CreateServer";

type CroppedAreaPixels = {
  x: number;
  y: number;
  width: number;
  height: number;
};

type ImageSelectionProps = {
  submitFunction: (imageUrl: string, crop: CroppedAreaPixels) => void;
};

export const ImageSelection = forwardRef<HTMLInputElement, ImageSelectionProps>(
  function ImageSelection({ submitFunction }, ref) {
    const [img, setImg] = useState<string | null>(null);
    const [crop, setCrop] = useState({ x: 0, y: 0 });
    const [zoom, setZoom] = useState(1);
    const [croppedAreaPixels, setCroppedAreaPixels] =
      useState<CroppedAreaPixels | null>(null);

    const handleSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files && e.target.files[0]) {
        if (img) {
          URL.revokeObjectURL(img);
        }

        const file = e.target.files[0];
        var fileUrl = URL.createObjectURL(file);
        setImg(fileUrl);
      }
    };

    const handlClose = () => {
      if (img) {
        URL.revokeObjectURL(img);
      }

      setImg(null);
      setCroppedAreaPixels(null);
    };

    const handleSubmit = () => {
      if (img && croppedAreaPixels) {
        submitFunction(img, croppedAreaPixels);
      }
    };

    return (
      <>
        <input
          type="file"
          ref={ref}
          onChange={handleSelect}
          accept="image/*"
          style={{ display: "none" }}
        />
        <AnimatePresence>
          {img && (
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
                    <X
                      className="image-cropper-close-btn"
                      onClick={handlClose}
                    />
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
                      onCropComplete={(_croppedArea, pixels) =>
                        setCroppedAreaPixels(pixels)
                      }
                    />
                  </div>
                  <div className="image-cropper-btns">
                    <button
                      className="form-submit-btn"
                      type="submit"
                      onClick={handleSubmit}
                      disabled={!croppedAreaPixels}
                    >
                      Definir nova foto de perfil
                    </button>
                  </div>
                </motion.div>
              </motion.div>
            </Portal>
          )}
        </AnimatePresence>
      </>
    );
  },
);

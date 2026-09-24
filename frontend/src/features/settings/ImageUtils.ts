export type Crop = {
  x: number;
  y: number;
  width: number;
  height: number;
};

const DEFAULT_SIZE = 256;
const DEFAULT_QUALITY = 0.9;
const MIME = "image/jpeg";

export function getCroppedImg(
  img: string,
  crop: Crop,
  size: number = DEFAULT_SIZE,
  quality: number = DEFAULT_QUALITY,
): Promise<File> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");

    if (!context) {
      reject(new Error("Não foi possível criar um constexto 2D no canvas"));
      return;
    }

    const image = new Image();

    image.onload = () => {
      canvas.width = size;
      canvas.height = size;

      context.drawImage(image, crop.x, crop.y, crop.width, crop.height, 0, 0, 256, 256);

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error("Não foi possível gerar a imagem recortada"));
            return;
          }
          const file = new File([blob], "profile_pic.jpg", { type: MIME });
          resolve(file);
        },
        MIME,
        quality,
      );
    };

    image.onerror = () => {
      reject(new Error("Não foi possível carregar a imagem para recorte"));
    };

    image.src = img;
  });
}

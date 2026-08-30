export type Crop = {
  x: number;
  y: number;
  width: number;
  height: number;
};

export function getCroppedImg(img: string, crop: Crop): Promise<File> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement("canvas");
    const context = canvas.getContext("2d");

    if (!context) {
      reject(new Error("Não foi possível criar um constexto 2D no canvas"));
      return;
    }

    const image = new Image();
    image.src = img;

    image.onload = () => {
      canvas.width = 256;
      canvas.height = 256;

      context.drawImage(
        image,
        crop.x,
        crop.y,
        crop.width,
        crop.height,
        0,
        0,
        256,
        256,
      );

      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error("Não foi possível gerar a imagem recortada"));
            return;
          }
          const file = new File([blob], "profile_pic.jpg", {
            type: "image/jpeg",
          });
          resolve(file);
        },
        "image/jpeg",
        0.9,
      );
    };

    image.onerror = () => {
      reject(new Error("Não foi possível carregar a imagem para recorte"));
    };
  });
}

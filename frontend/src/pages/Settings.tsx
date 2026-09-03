import { Upload, User } from "lucide-react";
import { useRef, useState } from "react";
import defaultProfilePic from "../assets/big-steve-face.png";
import { ImageSelection } from "../components/ImageSelection";
import InputItem from "../components/InputItem";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/apiClient";
import { getCroppedImg } from "../utils/ImageUtils";

export default function Settings() {
  const { user } = useAuth();

  const [username, setUsername] = useState<string>("");
  // const [email, setEmail] = useState<string>("");
  const [submitting, setSubmitting] = useState<boolean>(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (submitting || username === "") return;

    setSubmitting(true);

    api.user
      .update(user!.id, username)
      .catch((err) => {
        console.error("Erro ao atualizar o perfil do usuário: ", err);
        alert("Erro ao atualizar o perfil do usuário.");
      })
      .finally(() => {
        setSubmitting(false);
        window.location.reload();
      });
  };

  const handleCropAndUpload = async (
    img: string,
    crop: { x: number; y: number; width: number; height: number },
  ) => {
    if (!img || submitting) return;

    setSubmitting(true);

    const croppedImage = await getCroppedImg(img, crop);

    api.user
      .updateProfilePic(user!.id, croppedImage)
      .catch((err) => {
        console.error("Erro ao atualizar a foto de perfil do usuário: ", err);
        alert("Erro ao atualizar a foto de perfil.");
      })
      .finally(() => {
        setSubmitting(false);
        window.location.reload();
      });
  };

  return (
    <div className="settings-container">
      <div className="settings-user-container">
        <div className="settings-user-header">
          <h2>User settings</h2>
        </div>
        <div className="settings-user-content">
          <div className="settings-user-profile-pic-container">
            <div className="user-profile-picture">
              <img
                src={
                  user?.profilePictureUrl
                    ? user.profilePictureUrl
                    : defaultProfilePic
                }
                alt={`Foto de ${user?.username}`}
                className="picture"
              />
              <button
                className="picture-upload-overlay"
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload style={{ width: 48, height: 48 }} />
              </button>
              <ImageSelection
                ref={fileInputRef}
                submitFunction={handleCropAndUpload}
              />
            </div>
            <span className="user-profile-picture-title">
              <User style={{ width: 24, height: 24 }} /> {user?.username}
            </span>
          </div>
          <form
            className="settings-user-info-container"
            onSubmit={handleSubmit}
          >
            <InputItem
              type="text"
              title="Username:"
              defaultValue={user?.username}
              setFunction={setUsername}
            />
            <InputItem
              type="email"
              title="Email:"
              defaultValue={user?.email}
              setFunction={() => {}}
              disabled={true}
            />
            <button className="form-submit-btn" type="submit">
              Update Profile
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

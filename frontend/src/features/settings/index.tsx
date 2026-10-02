import { Upload, User } from "lucide-react";
import { useRef, useState } from "react";
import defaultProfilePic from "../../assets/big-steve-face.png";
import InputItem from "../../components/InputItem";
import SubmitButton from "../../components/SubmitButton";
import { useAuth } from "../../context/AuthContext";
import { ImageSelection } from "./components/ImageSelection";
import { useUserSettings } from "./hooks/useUserSettings";
import { getCroppedImg } from "./utils/ImageUtils";

export default function Settings() {
  const { user, refreshUser } = useAuth();
  const { isUpdatingUser, isUpdatingUserPicture, updateUser, updateUserProfilePic } = useUserSettings(user!.id);

  const [showImageSelection, setShowImageSelection] = useState<boolean>(false);
  const [username, setUsername] = useState<string>("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();

    if (isUpdatingUser || username === "") return;

    const success = await updateUser(username);
    if (success) await refreshUser();
  };

  const handleCropAndUpload = async (img: string, crop: { x: number; y: number; width: number; height: number }) => {
    if (!img || isUpdatingUserPicture) return;

    const croppedImage = await getCroppedImg(img, crop);

    const success = await updateUserProfilePic(croppedImage);
    if (success) await refreshUser();

    setShowImageSelection(false);
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
                src={user?.profilePictureUrl ? user.profilePictureUrl : defaultProfilePic}
                alt={`Foto de ${user?.username}`}
                className="picture"
              />
              <button
                className="picture-upload-overlay"
                onClick={() => {
                  setShowImageSelection(true);
                  fileInputRef.current?.click();
                }}
              >
                <Upload style={{ width: 48, height: 48 }} />
              </button>
              <ImageSelection
                ref={fileInputRef}
                isOpen={showImageSelection}
                onClose={() => setShowImageSelection(false)}
                submitFunction={handleCropAndUpload}
                isUpdatingPicture={isUpdatingUserPicture}
              />
            </div>
            <span className="user-profile-picture-title">
              <User style={{ width: 24, height: 24 }} /> {user?.username}
            </span>
          </div>
          <form className="settings-user-info-container" onSubmit={handleSubmit}>
            <InputItem type="text" title="Username:" defaultValue={user?.username} onChange={setUsername} />
            <InputItem type="email" title="Email:" defaultValue={user?.email} onChange={() => {}} disabled={true} />
            <div className="settings-submit-btn-container">
              <SubmitButton type="submit" label="Update Profile" loading={isUpdatingUser} />
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

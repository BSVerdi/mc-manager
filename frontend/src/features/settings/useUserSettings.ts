import { useCallback, useState } from "react";
import { userSettingsApi } from "./api";

export function useUserSettings(userId: number) {
  const [isUpdatingUser, setIsUpdatingUser] = useState<boolean>(false);
  const [isUpdatingUserPicture, setIsUpdationUserPicture] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const updateUser = useCallback(
    async (newUsername: string): Promise<boolean> => {
      setIsUpdatingUser(true);

      try {
        await userSettingsApi.update(userId, newUsername);

        return true;
      } catch (err) {
        console.error("[useUserSettings] Erro ao atualizar dados do usuário:", err);
        setError("Erro ao atualizar dados do usuário");

        return false;
      } finally {
        setIsUpdatingUser(false);
      }
    },
    [userId],
  );

  const updateUserProfilePic = useCallback(
    async (picture: File): Promise<boolean> => {
      setIsUpdationUserPicture(true);

      try {
        await userSettingsApi.updateProfilePic(userId, picture);

        return true;
      } catch (err) {
        console.error("[useUserSettings] Erro ao atualizar foto de perfil do usuário:", err);
        setError("Erro ao atualizar foto de perfil do usuário");

        return false;
      } finally {
        setIsUpdationUserPicture(false);
      }
    },
    [userId],
  );

  return {
    isUpdatingUser,
    isUpdatingUserPicture,
    error,
    updateUser,
    updateUserProfilePic,
  };
}

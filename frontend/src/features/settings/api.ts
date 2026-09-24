import { http } from "../../lib/apiClient";

export const userSettingsApi = {
  update: async (userId: number, username: string) =>
    http.patch(`/users/${userId}/update-user`, { username }).then((res) => res.data),

  updateProfilePic: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);

    return http.post(`/users/${userId}/profile-picture`, formData, {}).then((res) => res.data);
  },

  delete: async (userId: number) => http.delete(`/users/${userId}/delete`).then((res) => res.data),
};

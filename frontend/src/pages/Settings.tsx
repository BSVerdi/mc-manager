import { useAuth } from "../context/AuthContext";

export default function Settings() {
  const { user } = useAuth();

  return (
    <div className="settings-container">
      <div className="settings-user-container">
        <div className="settings-user-profile-pic">
          <img
            src="https://avatars.githubusercontent.com/u/170894112?v=4"
            alt=""
          />
          <span>{user?.username}</span>
        </div>
        <div>
          
        </div>
      </div>
    </div>
  );
}

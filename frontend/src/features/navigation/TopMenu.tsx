import { TOP_MENU_ITEMS } from "../../data/navigation";
import { NavItem } from "./NavItem";

export default function TopMenu() {
  return (
    <nav className="top-menu-container">
      <div className="top-menu-btns">
        {TOP_MENU_ITEMS.map((item) => (
          <NavItem key={item.id} item={item} />
        ))}
      </div>
    </nav>
  );
}

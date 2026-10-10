import { type Variants } from "framer-motion";

export const elevateIn: Variants = {
  hidden: { opacity: 0, y: 12 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { delay: 0.125 },
  },
  exit: { opacity: 0, y: 12 },
};

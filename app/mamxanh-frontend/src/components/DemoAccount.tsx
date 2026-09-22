import { createContext, useContext, useState, type ReactNode } from 'react';

// UI preview only: never represents a verified identity or server session.
const DemoAccountContext = createContext({ active: false, setActive: (_active: boolean) => {} });
export function DemoAccountProvider({ children }: { children: ReactNode }) {
  const [active, setActive] = useState(false);
  return <DemoAccountContext.Provider value={{ active, setActive }}>{children}</DemoAccountContext.Provider>;
}
export const useDemoAccount = () => useContext(DemoAccountContext);

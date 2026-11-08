import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { RoleProvider } from './hooks/useRole.jsx';
import { ConnectivityProvider } from './hooks/useConnectivity.jsx';
import { OutboxProvider } from './hooks/useOutbox.jsx';
import './styles/app.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <RoleProvider>
        <ConnectivityProvider>
          <OutboxProvider>
            <App />
          </OutboxProvider>
        </ConnectivityProvider>
      </RoleProvider>
    </BrowserRouter>
  </StrictMode>,
);

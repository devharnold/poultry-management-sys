import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import FarmsPage from './pages/FarmsPage.jsx';
import FlocksPage from './pages/FlocksPage.jsx';
import FlockDetailPage from './pages/FlockDetailPage.jsx';
import MortalityPage from './pages/MortalityPage.jsx';
import FeedPage from './pages/FeedPage.jsx';
import AlertsPage from './pages/AlertsPage.jsx';
import SyncPage from './pages/SyncPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="farms" element={<FarmsPage />} />
        <Route path="flocks" element={<FlocksPage />} />
        <Route path="flocks/:id" element={<FlockDetailPage />} />
        <Route path="mortality" element={<MortalityPage />} />
        <Route path="feed" element={<FeedPage />} />
        <Route path="alerts" element={<AlertsPage />} />
        <Route path="sync" element={<SyncPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

import { Link } from 'react-router-dom';
import { Empty } from '../components/Notice.jsx';

export default function NotFoundPage() {
  return (
    <Empty title="Page not found">
      <p>That address does not exist. <Link to="/">Go to the dashboard</Link>.</p>
    </Empty>
  );
}

import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from '../layouts/PublicLayout/PublicLayout.jsx'
import Home from '../pages/public/Home/Home.jsx'
import NotFound from '../pages/public/NotFound/NotFound.jsx'

// Central route definition. Protected/role-specific route groups are added
// in later phases behind layout routes (candidate, employer, admin, client).
const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])

export default router
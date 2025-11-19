import { Routes, Route } from 'react-router-dom'
import Home from '../views/Home'
import BeautyPreview from '../views/BeautyPreview'

function Router() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route 
        path="/camera" 
        element={<BeautyPreview />}
      />
    </Routes>
  )
}

export default Router


import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { RecipeModalProvider } from './contexts/RecipeModalContext';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RecipeModalProvider>
      <App />
    </RecipeModalProvider>
  </React.StrictMode>
);

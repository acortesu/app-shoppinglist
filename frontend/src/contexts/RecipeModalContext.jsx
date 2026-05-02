import { createContext, useState } from 'react';

export const RecipeModalContext = createContext();

export function RecipeModalProvider({ children }) {
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const openNewRecipe = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEditRecipe = (recipe) => {
    setEditing(recipe);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditing(null);
  };

  return (
    <RecipeModalContext.Provider value={{ showForm, setShowForm, editing, setEditing, openNewRecipe, openEditRecipe, closeForm }}>
      {children}
    </RecipeModalContext.Provider>
  );
}

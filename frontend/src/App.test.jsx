import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';

describe('App smoke', () => {
  it('renders auth gate title when auth is required', () => {
    render(<App />);
    expect(screen.getByText(/Iniciar sesión/i)).toBeInTheDocument();
  });
});

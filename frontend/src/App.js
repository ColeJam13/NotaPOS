import { useState } from 'react';
import Home from './views/Home';
import CreateOrder from './views/CreateOrder';
import ActiveOrders from './views/ActiveOrders';
import FloorMap from './views/FloorMap';
import ActiveTables from './views/ActiveTables';
import './App.css';

function App() {
  const [currentView, setCurrentView] = useState('home');
  const [selectedTable, setSelectedTable] = useState(null);

  if (currentView === 'createOrder') {
    return <CreateOrder setCurrentView={setCurrentView} selectedTable={selectedTable} />;
  }

  if (currentView === 'activeOrders') {
    return <ActiveOrders setCurrentView={setCurrentView} />;
  }

  if (currentView === 'floorMap') {
    return <FloorMap setCurrentView={setCurrentView} setSelectedTable={setSelectedTable} />;
  }

  if (currentView === 'activeTables') {
    return <ActiveTables setCurrentView={setCurrentView} setSelectedTable={setSelectedTable} />;
  }

  return <Home setCurrentView={setCurrentView} />;
}

export default App;
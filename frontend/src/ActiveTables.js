import { useState, useEffect } from 'react';
import NavBar from './NavBar';
import './App.css';

function ActiveTables({ setCurrentView, setSelectedTable }) {
    const [tables, setTables] = useState([]);
    const [orders, setOrders] = useState([]);
    const [orderItems, setOrderItems] = useState([]);

    useEffect(() => {                                                       // Load tables, orders, and order items on mount
        fetch('http://localhost:8080/api/tables')
            .then(response => response.json())
            .then(data => setTables(data))
            .catch(error => console.error('Error fetching tables:', error));

        fetch('http://localhost:8080/api/orders')
            .then(response => response.json())
            .then(data => setOrders(data))
            .catch(error => console.error('Error fetching orders:', error));

            fetch('http://localhost:8080/api/order-items')
                .then(response => response.json())
                .then(data => setOrderItems(data))
                .catch(error => console.error('Error fetching order items:', error));
    }, []);

    useEffect(() => {
                                                                            // Auto-refresh every 3 seconds
    const interval = setInterval(() => {
      fetch('http://localhost:8080/api/tables')
        .then(response => response.json())
        .then(data => setTables(data))
        .catch(error => console.error('Error fetching tables:', error));

      fetch('http://localhost:8080/api/orders')
        .then(response => response.json())
        .then(data => setOrders(data))
        .catch(error => console.error('Error fetching orders:', error));

      fetch('http://localhost:8080/api/order-items')
        .then(response => response.json())
        .then(data => setOrderItems(data))
        .catch(error => console.error('Error fetching order items:', error));
    }, 3000);

    return () => clearInterval(interval);
  }, []);

                                                                            // Filter to only occupied tables with open orders
  const occupiedTables = tables.filter(table => table.status === 'occupied');

                                                                            // Helper function to get order info for a table
  const getTableInfo = (table) => {
    const tableOrders = orders.filter(o => o.tableId === table.tableId && o.status === 'open');
    if (tableOrders.length === 0) return null;

                                                                            // Get all items for all orders for this table
    const allItems = tableOrders.flatMap(order => 
      orderItems.filter(item => item.orderId === order.orderId)
    );

                                                                            // Count items by status
    const limboCount = allItems.filter(item => item.status === 'limbo').length;
    const pendingCount = allItems.filter(item => item.status === 'pending').length;
    const firedCount = allItems.filter(item => item.status === 'fired').length;
    const completedCount = allItems.filter(item => item.status === 'completed').length;

                                                                            // Calculate total bill
    const total = allItems.reduce((sum, item) => sum + item.price, 0);

                                                                            // Get oldest order time
    const oldestOrder = tableOrders.sort((a, b) => a.createdAt - b.createdAt)[0];
    const timeAgo = getTimeAgo(oldestOrder.createdAt);

                                                                            // Determine border color based on status
    let borderColor = 'default';
    if (completedCount === allItems.length) borderColor = 'green';
    else if (limboCount > 0) borderColor = 'purple';
    else if (pendingCount > 0 || firedCount > 0) borderColor = 'yellow';

    return {
      total,
      itemCount: allItems.length,
      limboCount,
      pendingCount,
      firedCount,
      completedCount,
      timeAgo,
      borderColor
    };
  };

                                                                            // Helper to calculate time ago
const getTimeAgo = (timestamp) => {
    const now = Date.now();
    const created = new Date(timestamp).getTime();
    const diff = Math.floor((now - created) / 1000 / 60); // minutes
    if (diff < 60) return `${diff}m ago`;
    const hours = Math.floor(diff / 60);
    return `${hours}h ${diff % 60}m ago`;
    };

  return (
    <div className="page-with-nav">
      <NavBar currentView="activeTables" setCurrentView={setCurrentView} />
      <div className="active-tables-page">
        <h2>ACTIVE TABLES</h2>
        
        <div className="tables-grid">
          {occupiedTables.map(table => {
            const info = getTableInfo(table);
            if (!info) return null;

            return (
              <div 
                key={table.tableId} 
                className={`table-card border-${info.borderColor}`}
                onClick={() => {
                  setSelectedTable(table);
                  setCurrentView('createOrder');
                }}
              >
                <h3 className="table-card-number">{table.tableNumber}</h3>
                <div className="table-card-total">${info.total.toFixed(2)}</div>
                <div className="table-card-items">{info.itemCount} items</div>
                <div className="table-card-status">
                  {info.limboCount > 0 && `${info.limboCount}L `}
                  {info.pendingCount > 0 && `${info.pendingCount}P `}
                  {info.firedCount > 0 && `${info.firedCount}F `}
                  {info.completedCount > 0 && `${info.completedCount}C`}
                </div>
                <div className="table-card-server">Server: {table.server_name || table.serverName || 'N/A'}</div>
                <div className="table-card-time">{info.timeAgo}</div>
              </div>
            );
          })}
        </div>

        {occupiedTables.length === 0 && (
          <p className="no-tables">No active tables</p>
        )}
      </div>
    </div>
  );
}

export default ActiveTables;

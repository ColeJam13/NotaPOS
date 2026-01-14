import { useState, useEffect } from 'react';
import NavBar from './NavBar'
import { Lock } from 'lucide-react';
import './App.css';


function CreateOrder({ setCurrentView, selectedTable }) {
  const [menuItems, setMenuItems] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('Savory');
  const [orderItems, setOrderItems] = useState([])
  const [currentTableId, setCurrentTableId] = useState(selectedTable?.tableId || 1);
  const [timerExpires, setTimerExpires] = useState(null);
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [currentOrderId, setCurrentOrderId] = useState(null);

useEffect(() => {
  if (selectedTable && selectedTable.status === 'occupied') {
    fetch(`http://localhost:8080/api/orders?tableId=${selectedTable.tableId}`)
      .then(response => response.json())
      .then(orders => {
        if (orders.length > 0) {
          const activeOrder = orders.find(o => o.status === 'open');
          if (activeOrder) {
            setCurrentOrderId(activeOrder.orderId);

            fetch(`http://localhost:8080/api/order-items/order/${activeOrder.orderId}`)
              .then(response => response.json())
              .then(items => {
                console.log('Loaded items from backend:', items);
                
                fetch('http://localhost:8080/api/menu-items')
                  .then(response => response.json())
                  .then(menuData => {
                    const formattedItems = items.map(item => ({
                      orderItemId: item.orderItemId,
                      menuItemId: item.menuItemId,
                      name: menuData.find(m => m.menuItemId === item.menuItemId)?.name,
                      price: item.price,
                      quantity: item.quantity,
                      status: item.status
                    }));
                    console.log('Formatted items:', formattedItems);
                    setOrderItems(formattedItems);
                  });
              });
          }
        }
      })
      .catch(error => console.error('Error loading order:', error));
  }
}, [selectedTable]);

useEffect(() => {
  fetch('http://localhost:8080/api/menu-items')                             // fetch menu items
    .then(response => response.json())
    .then(data => {
      console.log('Menu items:', data);
      setMenuItems(data);
    })
    .catch(error => console.error('Error fetching menu items:', error));
  }, []);

  useEffect(() => {
    if (!timerExpires) return;                                       

    const interval = setInterval(() => {                              // timer logic
      const now = new Date();
      const diff = Math.floor((timerExpires - now) / 1000);

      if (diff <= 0) {
        console.log('Timer expired! Current items:', orderItems);  // ADD THIS
        setSecondsLeft(0);
        setTimerExpires(null);
        clearInterval(interval);

      setOrderItems(prevItems => prevItems.map(item => {
        if (item.status === 'limbo') return { ...item, status: 'pending' };
        return item;  // Don't touch draft/pending/fired/completed items
      }));

      } else {
        setSecondsLeft(diff);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [timerExpires, orderItems]);

  const sendOrder = async () => {
    try {

      const orderResponse = await fetch('http://localhost:8080/api/orders', {               // updates order status
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tableId: currentTableId,
          orderType: 'dine_in',
          status: 'open'
        })
      });
      const order = await orderResponse.json();
      console.log('Order created', order);
      setCurrentOrderId(order.orderId);

      await fetch(`http://localhost:8080/api/tables/${currentTableId}`, {                     // changes table status
        method: 'PUT',
        headers: { 'Content-Type': 'application/json'},
        body: JSON.stringify({ status: 'occupied' })
      });

      for (const item of orderItems) {
        await fetch('http://localhost:8080/api/order-items', {                              // Add item to order
          method: 'POST',
          headers: { 'Content-type': 'application/json' },
          body: JSON.stringify({
            orderId: order.orderId,
            menuItemId: item.menuItemId,
            quantity: item.quantity,
            price: item.price
          })
        });
      }

      const sendResponse = await fetch(`http://localhost:8080/api/order-items/order/${order.orderId}/send`, {         // send order
        method: 'POST'
      });
      const sentItems = await sendResponse.json();

      const expiresAt = new Date(sentItems[0].delayExpiresAt);
      setTimerExpires(expiresAt);
      setSecondsLeft(15);

      setOrderItems(prevItems => prevItems.map(item => {
        if (item.status === 'draft') return { ...item, status: 'limbo' };
        return item;  // Don't touch pending/fired/completed items
      }));

    } catch (error) {
      console.error('Error sending order:', error);
      alert('Failed to send order');
    }
  };

  return (                                                                          // change table in create order tab
    <div className="page-with-nav">
        <NavBar currentView="createOrder" setCurrentView={setCurrentView} />                      
            <div className="app">
            <div className="order-panel">
                <h2>Current Order - Table {selectedTable?.tableNumber || 'F1'}</h2>         

            {secondsLeft !== null && secondsLeft > 0 && (                           // display timer
                <div className="timer-display">
                {secondsLeft} seconds to edit
                </div>
            )}

            {secondsLeft === 0 && (                                                   // lock item
                <div className="timer-locked">
              Items locked and sent to prep station                                   
                </div>
            )}

                <div className="order-items-list">
                {orderItems.map((item, index) => (                                                              // lock item and change status
                    <div key={index} className={`order-item ${(item.status === 'pending' || item.status === 'fired' || item.status === 'completed') ? 'locked' : ''}`}>               
                    <span>
                      {(item.status === 'pending' || item.status === 'fired' || item.status === 'completed') && <Lock size={14} className="lock-icon" />}
                      {item.name}
                    </span>
                    <span>${item.price.toFixed(2)}</span>
                    {(item.status === 'draft' || item.status === 'limbo') &&(
                    <button
                        className="btn-remove"                                                      // remove item
                        onClick={() => {
                        setOrderItems(orderItems.filter((_, i) => i !== index));

                        if (timerExpires) {
                            const newExpires = new Date(Date.now() + 15000);
                            setTimerExpires(newExpires);
                            setSecondsLeft(15);
                        }
                        }}
                    >
                        x
                    </button>
                    )}
                    </div>
                ))}
                </div>

                {orderItems.length === 0 && (                                             // show when no items on order / calculate totals (below)
                <p className="empty-order">No items added yet</p>
                )}
                                                                                                    
            <div className="order-totals">                                                          
                <div className="total-row">
                <span>Subtotal:</span>
                <span>${orderItems.reduce((sum, item) => sum + item.price, 0).toFixed(2)}</span>
                </div>
                <div className="total-row"> 
                <span>Tax (3%):</span>
                <span>${(orderItems.reduce((sum, item) => sum + item.price, 0) * 0.03).toFixed(2)}</span>
                </div>
                <div className="total-row total">
                <span>TOTAL:</span>
                <span>${(orderItems.reduce((sum, item) => sum + item.price, 0) * 1.03).toFixed(2)}</span>
                </div>
            </div>
                <div className="order-actions">
                <button className="btn-save">SAVE DRAFT</button>
                <button className="btn-send" onClick={async () => {
                    if (timerExpires && currentOrderId) {

                      await fetch(`http://localhost:8080/api/order-items/order/${currentOrderId}/send-now`, {                 // send order now (bypass timer)
                        method: 'POST'
                      });

                    setSecondsLeft(0);
                    setTimerExpires(null);
                    setOrderItems(prevItems => prevItems.map(item => {
                      if (item.status === 'limbo') return { ...item, status: 'pending' };
                      return item;  // Don't touch draft/pending/fired/completed items
                    }));
                    } else {
                    sendOrder();
                    }
                }}>
                    {timerExpires ? 'SEND NOW?' : 'SEND ORDER'}
                </button>
                </div>
            </div>
                                                                                  
            <div className="menu-panel">                                                          
                <div className="category-tabs">
                <button
                    className={`category-tab ${selectedCategory === 'Savory' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Savory')}                                       // Menu Category panel and buttons
                >
                    SAVORY
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Sweet' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Sweet')}
                >
                    SWEET
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Build Your Own' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Build Your Own')}
                >
                    BUILD YOUR OWN
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Snacks & Sides' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Snacks & Sides')}
                >
                    SNACKS & SIDES
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Beverages' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Beverages')}
                >
                    BEVERAGES
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Cocktails' ? 'active' : ''}`}
                    onClick={() => setSelectedCategory('Cocktails')}
                >
                    COCKTAILS
                </button>
                <button
                    className={`category-tab ${selectedCategory === 'Coffee' ? 'active' : ''}}`}
                    onClick={() => setSelectedCategory('Coffee')}
                >
                    COFFEE
                </button>
                </div>
                <div className="menu-grid">
                {menuItems
                    .filter(item => item.category === selectedCategory)                             // maps the items to their buttons
                    .map(item => (
                    <div key ={item.menuItemId} className="menu-item-card" onClick={() => {
                        setOrderItems([...orderItems, {
                        menuItemId: item.menuItemId,
                        name: item.name,
                        price: item.price,
                        quantity: 1,
                        status: 'draft'
                        }]);

                        if (timerExpires) {
                        const newExpires = new Date(Date.now() + 15000);
                        setTimerExpires(newExpires);
                        setSecondsLeft(15);
                        }

                        if (secondsLeft === 0) {
                        setSecondsLeft(null);
                        }
                    }}>
                        <h3>{item.name}</h3>
                        <p className="price">${item.price.toFixed(2)}</p>
                    </div>
                    ))
                }
                </div>
            </div>
        </div>
    </div>
  );
}

export default CreateOrder;
import { useEffect, useMemo, useState } from 'react'
import {
  ArrowLeft, BadgeCheck, Heart, LayoutDashboard, LoaderCircle, LogOut, Minus,
  CreditCard, Package, Plus, Search, ShoppingBag, Trash2, UserRound
} from 'lucide-react'
import { api } from './api'

const emptyProduct = {
  name: '', brand: '', category: 'Women', description: '', imageUrl: '',
  price: '', originalPrice: '', stock: 1, featured: false
}

const money = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 0
}).format(Number(value || 0))

export default function App() {
  const [view, setView] = useState('shop')
  const [products, setProducts] = useState([])
  const [selected, setSelected] = useState(null)
  const [cart, setCart] = useState({ items: [], subtotal: 0, count: 0 })
  const [user, setUser] = useState(null)
  const [authOpen, setAuthOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('')

  useEffect(() => {
    bootstrap()
  }, [])

  async function bootstrap() {
    setLoading(true)
    const [productResult, cartResult, userResult] = await Promise.allSettled([api.products(), api.cart(), api.me()])
    if (productResult.status === 'fulfilled') setProducts(productResult.value.products)
    if (cartResult.status === 'fulfilled') setCart(cartResult.value)
    if (userResult.status === 'fulfilled') setUser(userResult.value.user)
    if (productResult.status === 'rejected') setError(productResult.reason.message)
    setLoading(false)
  }

  async function loadProducts(nextQuery = query, nextCategory = category) {
    await run(async () => {
      const params = new URLSearchParams()
      if (nextQuery) params.set('q', nextQuery)
      if (nextCategory) params.set('category', nextCategory)
      const data = await api.products(params.size ? `?${params}` : '')
      setProducts(data.products)
    })
  }

  async function openProduct(id) {
    await run(async () => {
      setSelected((await api.product(id)).product)
      setView('detail')
    })
  }

  async function mutateCart(work) {
    await run(async () => setCart(await work()))
  }

  async function run(work) {
    setBusy(true)
    setError('')
    try {
      await work()
    } catch (exception) {
      setError(exception.message)
    } finally {
      setBusy(false)
    }
  }

  async function signOut() {
    await run(async () => {
      await api.logout()
      setUser(null)
      setView('shop')
    })
  }

  return (
    <div className="min-h-screen bg-linen text-ink">
      <Header user={user} count={cart.count} setView={setView} setAuthOpen={setAuthOpen} signOut={signOut} />
      {error && <div role="alert" className="mx-auto mt-4 max-w-7xl rounded-md bg-rose-100 px-4 py-3 text-sm text-rose-900">{error}</div>}
      {loading ? <Loading /> : (
        <main>
          {view === 'shop' && <Shop products={products} query={query} category={category} busy={busy}
            setQuery={setQuery} setCategory={setCategory} loadProducts={loadProducts}
            openProduct={openProduct} add={(id) => mutateCart(() => api.addCart(id))} />}
          {view === 'detail' && selected && <Details product={selected} back={() => setView('shop')}
            add={(id) => mutateCart(() => api.addCart(id))} />}
          {view === 'cart' && <Cart cart={cart} user={user} setAuthOpen={setAuthOpen}
            update={(id, quantity) => mutateCart(() => api.updateCart(id, quantity))}
            remove={(id) => mutateCart(() => api.removeCart(id))}
            ordered={() => api.cart().then(setCart)} />}
          {view === 'account' && <Account user={user} setUser={setUser} setAuthOpen={setAuthOpen} />}
          {view === 'admin' && <Admin user={user} products={products} reload={() => loadProducts()} />}
        </main>
      )}
      {busy && <div className="fixed bottom-4 right-4 flex items-center gap-2 rounded-md bg-ink px-4 py-3 text-sm text-white shadow-polish"><LoaderCircle className="h-4 w-4 animate-spin" /> Updating</div>}
      {authOpen && <AuthDialog close={() => setAuthOpen(false)} setUser={setUser} />}
    </div>
  )
}

function Header({ user, count, setView, setAuthOpen, signOut }) {
  return (
    <header className="sticky top-0 z-20 border-b border-stone-200/80 bg-linen/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6">
        <button onClick={() => setView('shop')} className="text-left" aria-label="Open storefront">
          <span className="block text-2xl font-black">STYLE SPHERE</span>
          <span className="block text-[11px] font-bold uppercase text-mint">Shop Now</span>
        </button>
        <nav className="flex items-center gap-2">
          <IconButton title="Shop" onClick={() => setView('shop')}><ShoppingBag /></IconButton>
          <IconButton title="Cart" onClick={() => setView('cart')}>
            <ShoppingBag /><span className="min-w-5 rounded-full bg-coral px-1 text-xs text-white">{count}</span>
          </IconButton>
          {user ? (
            <>
              <IconButton title="Profile and orders" onClick={() => setView('account')}><UserRound /></IconButton>
              {user.role === 'ADMIN' && <IconButton title="Admin dashboard" onClick={() => setView('admin')}><LayoutDashboard /></IconButton>}
              <IconButton title="Sign out" onClick={signOut}><LogOut /></IconButton>
            </>
          ) : <button className="action" onClick={() => setAuthOpen(true)}>Sign in</button>}
        </nav>
      </div>
    </header>
  )
}

function IconButton({ children, title, onClick }) {
  return <button title={title} aria-label={title} onClick={onClick} className="ghost h-11 w-auto px-3 [&_svg]:h-4 [&_svg]:w-4">{children}</button>
}

function Loading() {
  return <div className="grid min-h-[70vh] place-items-center"><LoaderCircle className="h-9 w-9 animate-spin text-coral" /></div>
}

function Shop({ products, query, category, setQuery, setCategory, loadProducts, openProduct, add }) {
  const categories = ['', 'Women', 'Men', 'Footwear', 'Accessories']
  return (
    <>
      <section className="relative isolate min-h-[min(76vh,760px)] overflow-hidden">
        <img className="absolute inset-0 h-full w-full object-cover" alt="Fashion editorial collection"
          src="https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=1800&q=80" />
        <div className="absolute inset-0 bg-gradient-to-r from-black/75 via-black/40 to-transparent" />
        <div className="relative mx-auto flex min-h-[min(76vh,760px)] max-w-7xl items-center px-4 pb-16 pt-10 text-white sm:px-6">
          <div className="max-w-2xl">
            <p className="eyebrow text-rose-200">New season wardrobe</p>
            <h1 className="mt-4 text-5xl font-black leading-tight sm:text-7xl">STYLE SPHERE</h1>
            <p className="mt-5 max-w-xl text-base text-stone-100 sm:text-xl">Sharp layers, fluid dresses, and accessories curated for a Myntra-style high-energy fashion shop.</p>
            <form className="mt-7 flex max-w-xl flex-col gap-2 rounded-md bg-white p-2 text-ink sm:flex-row"
              onSubmit={(event) => { event.preventDefault(); loadProducts() }}>
              <label className="flex flex-1 items-center gap-2 px-2">
                <Search className="h-4 w-4" /><input className="w-full py-2 outline-none" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search brand or style" />
              </label>
              <button className="action">Search</button>
            </form>
          </div>
        </div>
      </section>
      <section className="-mt-10 bg-white py-10">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="mb-6 flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div><p className="eyebrow">Trending now</p><h2 className="mt-2 text-3xl font-black">Shop Now</h2></div>
            <div className="flex flex-wrap gap-2">
              {categories.map((item) => <button key={item || 'All'} onClick={() => { setCategory(item); loadProducts(query, item) }}
                className={category === item ? 'action' : 'ghost'}>{item || 'All'}</button>)}
            </div>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {products.map((product) => <ProductCard key={product.id} product={product} openProduct={openProduct} add={add} />)}
          </div>
          {!products.length && <p className="py-16 text-center text-stone-500">No styles matched that filter.</p>}
        </div>
      </section>
    </>
  )
}

function ProductCard({ product, openProduct, add }) {
  return (
    <article className="group overflow-hidden rounded-lg border border-stone-200 bg-linen">
      <button className="block aspect-[4/5] w-full overflow-hidden" onClick={() => openProduct(product.id)} aria-label={`View ${product.name}`}>
        <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
      </button>
      <div className="space-y-3 p-4">
        <div className="flex justify-between gap-3">
          <div><p className="text-xs font-bold uppercase text-mint">{product.brand}</p><h3 className="mt-1 font-bold">{product.name}</h3></div>
          {product.featured && <BadgeCheck className="h-5 w-5 shrink-0 text-coral" />}
        </div>
        <div className="flex items-center gap-2"><strong>{money(product.price)}</strong><span className="text-sm text-stone-500 line-through">{money(product.originalPrice)}</span></div>
        <div className="flex gap-2"><button className="action flex-1" disabled={!product.stock} onClick={() => add(product.id)}><ShoppingBag className="h-4 w-4" />Add</button><button title="Open details" className="ghost" onClick={() => openProduct(product.id)}><Heart className="h-4 w-4" /></button></div>
      </div>
    </article>
  )
}

function Details({ product, back, add }) {
  return (
    <section className="mx-auto grid max-w-7xl gap-8 px-4 py-8 sm:px-6 lg:grid-cols-2">
      <img src={product.imageUrl} alt={product.name} className="aspect-[4/5] w-full rounded-lg object-cover" />
      <div className="flex flex-col justify-center">
        <button onClick={back} className="ghost mb-8 w-fit"><ArrowLeft className="h-4 w-4" />Back</button>
        <p className="eyebrow">{product.category} / {product.brand}</p>
        <h1 className="mt-3 text-4xl font-black">{product.name}</h1>
        <p className="mt-5 text-lg leading-8 text-stone-600">{product.description}</p>
        <div className="mt-6 flex items-end gap-3 text-2xl"><strong>{money(product.price)}</strong><span className="text-base text-stone-500 line-through">{money(product.originalPrice)}</span></div>
        <p className="mt-3 text-sm font-semibold text-mint">{product.stock} pieces ready to ship</p>
        <button className="action mt-8 w-full sm:w-fit" disabled={!product.stock} onClick={() => add(product.id)}><ShoppingBag className="h-4 w-4" />Add to bag</button>
      </div>
    </section>
  )
}

function Cart({ cart, user, setAuthOpen, update, remove, ordered }) {
  const [checkout, setCheckout] = useState({ shippingName: user?.name || '', shippingPhone: user?.phone || '', shippingAddress: address(user) })
  const [orders, setOrders] = useState([])
  const [message, setMessage] = useState('')
  const [problem, setProblem] = useState('')
  const [paying, setPaying] = useState(false)

  async function placeOrder(event) {
    event.preventDefault()
    setProblem('')
    if (!user) {
      setAuthOpen(true)
      return
    }
    try {
      setPaying(true)
      const data = await api.checkout(checkout)
      if (!data.razorpayOrderId) {
        setOrders([data.order, ...orders])
        setMessage(data.message || `Order #${data.order.id} created. Payment is pending.`)
        return
      }
      await openRazorpay(data)
    } catch (exception) {
      setProblem(exception.message)
    } finally {
      setPaying(false)
    }
  }

  async function openRazorpay(data) {
    await loadRazorpay()
    const order = data.order
    return new Promise((resolve, reject) => {
      const checkout = new window.Razorpay({
        key: data.razorpayKeyId,
        amount: data.amount,
        currency: data.currency,
        name: 'STYLE SPHERE',
        description: `Order #${order.id}`,
        order_id: data.razorpayOrderId,
        prefill: { name: order.shippingName, contact: order.shippingPhone },
        handler: async (payment) => {
          try {
            const verified = await api.verifyPayment({
              orderId: order.id,
              razorpayPaymentId: payment.razorpay_payment_id,
              razorpayOrderId: payment.razorpay_order_id,
              razorpaySignature: payment.razorpay_signature
            })
            setOrders([verified.order, ...orders])
            setMessage(`Payment successful. Order #${verified.order.id} is confirmed.`)
            ordered()
            resolve()
          } catch (exception) {
            reject(exception)
          }
        },
        modal: {
          ondismiss: async () => {
            await api.failPayment({ orderId: order.id, reason: 'Razorpay checkout closed.' }).catch(() => {})
            reject(new Error('Payment was not completed.'))
          }
        },
        theme: { color: '#c8553d' }
      })
      checkout.on('payment.failed', async (response) => {
        await api.failPayment({ orderId: order.id, reason: response.error?.description || 'Payment failed.' }).catch(() => {})
        reject(new Error(response.error?.description || 'Payment failed.'))
      })
      checkout.open()
    })
  }

  return (
    <section className="mx-auto grid max-w-7xl gap-8 px-4 py-8 sm:px-6 lg:grid-cols-[1.35fr_.65fr]">
      <div>
        <p className="eyebrow">Shopping bag</p>
        <h1 className="mt-2 text-4xl font-black">Cart</h1>
        <div className="mt-6 space-y-4">
          {cart.items.map(({ product, quantity, lineTotal }) => (
            <article key={product.id} className="grid grid-cols-[96px_1fr] gap-4 border-b border-stone-200 bg-white p-3 sm:grid-cols-[120px_1fr_auto]">
              <img src={product.imageUrl} alt={product.name} className="aspect-[4/5] w-full rounded-md object-cover" />
              <div className="min-w-0">
                <p className="text-xs font-bold uppercase text-mint">{product.brand}</p>
                <h2 className="mt-1 font-bold">{product.name}</h2>
                <p className="mt-2 font-semibold">{money(lineTotal)}</p>
                <div className="mt-3 flex items-center gap-2">
                  <IconButton title="Decrease quantity" onClick={() => update(product.id, quantity - 1)}><Minus /></IconButton>
                  <span className="grid h-11 min-w-11 place-items-center rounded-md border border-stone-200 bg-linen">{quantity}</span>
                  <IconButton title="Increase quantity" onClick={() => update(product.id, quantity + 1)}><Plus /></IconButton>
                </div>
              </div>
              <IconButton title="Remove item" onClick={() => remove(product.id)}><Trash2 /></IconButton>
            </article>
          ))}
          {!cart.items.length && <p className="rounded-md border border-stone-200 bg-white p-8 text-stone-500">Your bag is empty.</p>}
        </div>
      </div>
      <form onSubmit={placeOrder} className="h-fit rounded-lg border border-stone-200 bg-white p-5 shadow-polish">
        <p className="eyebrow">Checkout</p>
        <div className="mt-3 flex items-baseline justify-between"><h2 className="text-2xl font-black">Total</h2><strong className="text-2xl">{money(cart.subtotal)}</strong></div>
        <div className="mt-5 space-y-3">
          <input className="field" required placeholder="Shipping name" value={checkout.shippingName} onChange={(event) => setCheckout({ ...checkout, shippingName: event.target.value })} />
          <input className="field" required placeholder="Phone" value={checkout.shippingPhone} onChange={(event) => setCheckout({ ...checkout, shippingPhone: event.target.value })} />
          <textarea className="field min-h-28" required placeholder="Full delivery address" value={checkout.shippingAddress} onChange={(event) => setCheckout({ ...checkout, shippingAddress: event.target.value })} />
        </div>
        <button className="action mt-4 w-full" disabled={!cart.items.length || paying}>{paying ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}{paying ? 'Opening payment...' : 'Pay Now'}</button>
        {message && <p className="mt-3 rounded-md bg-emerald-100 p-3 text-sm text-emerald-900">{message}</p>}
        {problem && <p className="mt-3 rounded-md bg-rose-100 p-3 text-sm text-rose-900">{problem}</p>}
      </form>
    </section>
  )
}

function AuthDialog({ close, setUser }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [problem, setProblem] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setProblem('')
    try {
      const data = mode === 'login' ? await api.login(form) : await api.register(form)
      setUser(data.user)
      close()
    } catch (exception) {
      setProblem(exception.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-30 grid place-items-center bg-black/60 p-4" role="dialog" aria-modal="true">
      <form onSubmit={submit} className="w-full max-w-md rounded-lg bg-white p-6 shadow-polish">
        <div className="flex items-start justify-between gap-4">
          <div><p className="eyebrow">Member access</p><h2 className="mt-2 text-3xl font-black">{mode === 'login' ? 'Sign in' : 'Create account'}</h2></div>
          <button type="button" className="ghost min-h-0 px-3 py-2" onClick={close}>Close</button>
        </div>
        <div className="mt-5 space-y-3">
          {mode === 'register' && <input className="field" required placeholder="Full name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />}
          <input className="field" required type="email" placeholder="Email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
          <input className="field" required minLength={8} type="password" placeholder="Password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
        </div>
        <button className="action mt-4 w-full" disabled={busy}>{busy ? 'Working...' : mode === 'login' ? 'Sign in' : 'Register'}</button>
        <button type="button" className="mt-4 text-sm font-semibold text-coral" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'Need an account? Register' : 'Already registered? Sign in'}
        </button>
        {problem && <p className="mt-3 rounded-md bg-rose-100 p-3 text-sm text-rose-900">{problem}</p>}
      </form>
    </div>
  )
}

function Account({ user, setUser, setAuthOpen }) {
  const [profile, setProfile] = useState(user || {})
  const [orders, setOrders] = useState([])
  const [status, setStatus] = useState({ loading: Boolean(user), message: '', problem: '' })

  useEffect(() => {
    if (!user) return
    Promise.all([api.profile(), api.orders()])
      .then(([profileData, orderData]) => {
        setProfile(profileData.user)
        setOrders(orderData.orders)
        setStatus({ loading: false, message: '', problem: '' })
      })
      .catch((exception) => setStatus({ loading: false, message: '', problem: exception.message }))
  }, [user])

  async function save(event) {
    event.preventDefault()
    try {
      const data = await api.saveProfile(profile)
      setProfile(data.user)
      setUser(data.user)
      setStatus({ loading: false, message: 'Profile saved.', problem: '' })
    } catch (exception) {
      setStatus({ loading: false, message: '', problem: exception.message })
    }
  }

  if (!user) return <Gate open={setAuthOpen} text="Sign in to manage your profile and orders." />
  return (
    <section className="mx-auto grid max-w-7xl gap-8 px-4 py-8 sm:px-6 lg:grid-cols-[.8fr_1.2fr]">
      <form onSubmit={save} className="space-y-3 rounded-lg border border-stone-200 bg-white p-5">
        <p className="eyebrow">User profile</p><h1 className="text-3xl font-black">Hello, {user.name}</h1>
        <input className="field" value={profile.name || ''} onChange={(event) => setProfile({ ...profile, name: event.target.value })} />
        <input className="field bg-stone-100" value={profile.email || ''} readOnly />
        <input className="field" placeholder="Phone" value={profile.phone || ''} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} />
        <input className="field" placeholder="Address line" value={profile.addressLine || ''} onChange={(event) => setProfile({ ...profile, addressLine: event.target.value })} />
        <div className="grid gap-3 sm:grid-cols-3">
          <input className="field" placeholder="City" value={profile.city || ''} onChange={(event) => setProfile({ ...profile, city: event.target.value })} />
          <input className="field" placeholder="State" value={profile.state || ''} onChange={(event) => setProfile({ ...profile, state: event.target.value })} />
          <input className="field" placeholder="PIN" value={profile.postalCode || ''} onChange={(event) => setProfile({ ...profile, postalCode: event.target.value })} />
        </div>
        <button className="action">Save profile</button>
        {status.message && <p className="text-sm text-mint">{status.message}</p>}
        {status.problem && <p className="text-sm text-rose-700">{status.problem}</p>}
      </form>
      <div>
        <p className="eyebrow">Orders</p><h2 className="mt-2 text-3xl font-black">Purchase history</h2>
        <div className="mt-5 space-y-4">
          {status.loading && <p>Loading orders...</p>}
          {orders.map((order) => <article key={order.id} className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="flex flex-wrap justify-between gap-2"><strong>Order #{order.id}</strong><span>{order.status} / {money(order.total)}</span></div>
            <p className="mt-2 text-sm text-stone-500">{order.shippingAddress}</p>
            <div className="mt-3 flex flex-wrap gap-2">{order.items.map((item) => <span key={`${order.id}-${item.productId}`} className="rounded-md bg-linen px-2 py-1 text-sm">{item.productName} x {item.quantity}</span>)}</div>
          </article>)}
          {!status.loading && !orders.length && <p className="rounded-md border border-stone-200 bg-white p-6 text-stone-500">Checkout orders will appear here.</p>}
        </div>
      </div>
    </section>
  )
}

function Admin({ user, products, reload }) {
  const [summary, setSummary] = useState(null)
  const [editing, setEditing] = useState(emptyProduct)
  const [message, setMessage] = useState('')
  const [problem, setProblem] = useState('')
  const isAdmin = user?.role === 'ADMIN'

  useEffect(() => {
    if (isAdmin) api.adminSummary().then(setSummary).catch((exception) => setProblem(exception.message))
  }, [isAdmin])

  async function save(event) {
    event.preventDefault()
    setProblem('')
    try {
      const payload = {
        ...editing,
        price: Number(editing.price),
        originalPrice: editing.originalPrice === '' ? null : Number(editing.originalPrice),
        stock: Number(editing.stock)
      }
      if (editing.id) await api.updateProduct(editing.id, payload)
      else await api.createProduct(payload)
      setEditing(emptyProduct)
      setMessage('Product catalog updated.')
      reload()
    } catch (exception) {
      setProblem(exception.message)
    }
  }

  async function remove(id) {
    setProblem('')
    try {
      await api.deleteProduct(id)
      reload()
    } catch (exception) {
      setProblem(exception.message)
    }
  }

  if (!isAdmin) return <Gate text="Admin credentials are required for catalog management." />
  return (
    <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <p className="eyebrow">Operations</p><h1 className="mt-2 text-4xl font-black">Admin dashboard</h1>
      <div className="mt-6 grid gap-3 sm:grid-cols-4">
        {Object.entries(summary || { products: '...', customers: '...', orders: '...', revenue: '...' }).filter(([, value]) => !Array.isArray(value)).map(([label, value]) => (
          <div key={label} className="rounded-lg border border-stone-200 bg-white p-4">
            <p className="text-xs font-bold uppercase text-stone-500">{label}</p>
            <strong className="mt-2 block text-2xl">{label === 'revenue' && value !== '...' ? money(value) : value}</strong>
          </div>
        ))}
      </div>
      {summary && <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-stone-200 bg-white p-4">
          <h2 className="text-lg font-black">Payment statuses</h2>
          <div className="mt-3 space-y-2">{summary.paymentStatuses?.map((item) => <div key={item.status} className="flex justify-between text-sm"><span>{item.status}</span><strong>{item.total}</strong></div>)}</div>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white p-4">
          <h2 className="text-lg font-black">Recent orders</h2>
          <div className="mt-3 space-y-2">{summary.recentOrders?.map((order) => <div key={order.id} className="flex justify-between gap-3 text-sm"><span className="truncate">#{order.orderNumber} / {order.customer}</span><strong>{order.paymentStatus} / {money(order.total)}</strong></div>)}</div>
        </div>
      </div>}
      <div className="mt-8 grid gap-6 lg:grid-cols-[.8fr_1.2fr]">
        <form onSubmit={save} className="space-y-3 rounded-lg border border-stone-200 bg-white p-5">
          <h2 className="text-2xl font-black">{editing.id ? 'Edit product' : 'Add product'}</h2>
          <input required className="field" placeholder="Name" value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} />
          <div className="grid gap-3 sm:grid-cols-2">
            <input required className="field" placeholder="Brand" value={editing.brand} onChange={(event) => setEditing({ ...editing, brand: event.target.value })} />
            <select className="field" value={editing.category} onChange={(event) => setEditing({ ...editing, category: event.target.value })}>
              <option>Women</option><option>Men</option><option>Accessories</option><option>Footwear</option>
            </select>
          </div>
          <textarea required className="field min-h-24" placeholder="Description" value={editing.description} onChange={(event) => setEditing({ ...editing, description: event.target.value })} />
          <input required className="field" type="url" placeholder="Image URL" value={editing.imageUrl} onChange={(event) => setEditing({ ...editing, imageUrl: event.target.value })} />
          <div className="grid gap-3 sm:grid-cols-3">
            <input required className="field" min="1" type="number" placeholder="Price" value={editing.price} onChange={(event) => setEditing({ ...editing, price: event.target.value })} />
            <input className="field" min="1" type="number" placeholder="MRP" value={editing.originalPrice || ''} onChange={(event) => setEditing({ ...editing, originalPrice: event.target.value })} />
            <input required className="field" min="0" type="number" placeholder="Stock" value={editing.stock} onChange={(event) => setEditing({ ...editing, stock: event.target.value })} />
          </div>
          <label className="flex items-center gap-2 text-sm font-semibold"><input type="checkbox" checked={editing.featured} onChange={(event) => setEditing({ ...editing, featured: event.target.checked })} />Featured style</label>
          <div className="flex gap-2"><button className="action">{editing.id ? 'Save' : 'Create'}</button><button type="button" className="ghost" onClick={() => setEditing(emptyProduct)}>Reset</button></div>
          {message && <p className="text-sm text-mint">{message}</p>}
          {problem && <p className="text-sm text-rose-700">{problem}</p>}
        </form>
        <div className="overflow-hidden rounded-lg border border-stone-200 bg-white">
          <div className="grid grid-cols-[1fr_auto_auto] gap-3 border-b border-stone-200 p-4 text-sm font-bold"><span>Catalog</span><span>Stock</span><span>Actions</span></div>
          {products.map((product) => <div key={product.id} className="grid grid-cols-[1fr_auto_auto] items-center gap-3 border-b border-stone-100 p-4 last:border-b-0">
            <div className="flex min-w-0 items-center gap-3"><img src={product.imageUrl} alt="" className="h-16 w-14 rounded-md object-cover" /><div className="min-w-0"><strong className="block truncate">{product.name}</strong><span className="text-sm text-stone-500">{product.brand} / {money(product.price)}</span></div></div>
            <span>{product.stock}</span>
            <div className="flex gap-2"><button className="ghost min-h-0 px-3 py-2" onClick={() => setEditing(product)}>Edit</button><IconButton title={`Delete ${product.name}`} onClick={() => remove(product.id)}><Trash2 /></IconButton></div>
          </div>)}
        </div>
      </div>
    </section>
  )
}

function Gate({ open, text }) {
  return (
    <section className="mx-auto max-w-3xl px-4 py-24 text-center sm:px-6">
      <p className="eyebrow">Restricted</p>
      <h1 className="mt-3 text-4xl font-black">{text}</h1>
      {open && <button className="action mt-6" onClick={() => open(true)}>Sign in</button>}
    </section>
  )
}

function address(user) {
  if (!user) return ''
  return [user.addressLine, user.city, user.state, user.postalCode].filter(Boolean).join(', ')
}

function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload = resolve
    script.onerror = () => reject(new Error('Razorpay checkout could not be loaded.'))
    document.body.appendChild(script)
  })
}

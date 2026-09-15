import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import './App.css'

type User = { userId: number; email: string; name: string }
type Product = {
  productId: number
  name: string
  price: number
  genre: string
  stock: number
  image?: string
  description?: string
}
type CartItem = { cartId: number; product: Product; quantity: number; subtotal: number }
type Cart = { items: CartItem[]; totalAmount: number }
type Purchase = CartItem & { purchaseId: number; price: number; purchasedAt: string }
type MeResponse = { authenticated: boolean; user: User | null }

let csrfToken = ''

async function refreshCsrf() {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' })
  const data = await response.json() as { token: string }
  csrfToken = data.token
}

async function api<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const isMutation = method !== 'GET' && method !== 'HEAD'
  if (isMutation && !csrfToken) await refreshCsrf()

  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (isMutation && csrfToken) headers.set('X-XSRF-TOKEN', csrfToken)

  const response = await fetch(path, { ...options, headers, credentials: 'include' })
  if (response.status === 403 && isMutation && retry) {
    await refreshCsrf()
    return api<T>(path, options, false)
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '通信に失敗しました。' })) as { message?: string }
    throw new Error(error.message ?? '通信に失敗しました。')
  }
  if (response.status === 204) return undefined as T
  return await response.json() as T
}

function formatYen(value: number) {
  return `${value.toLocaleString('ja-JP')}円`
}

function useRoute() {
  const [path, setPath] = useState(window.location.pathname)
  const navigate = useCallback((to: string) => {
    window.history.pushState({}, '', to)
    setPath(to)
    window.scrollTo(0, 0)
  }, [])

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  return { path, navigate }
}

function App() {
  const { path, navigate } = useRoute()
  const [user, setUser] = useState<User | null>(null)
  const [authLoading, setAuthLoading] = useState(true)
  const [globalMessage, setGlobalMessage] = useState('')

  useEffect(() => {
    Promise.all([
      refreshCsrf(),
      api<MeResponse>('/api/auth/me'),
    ]).then(([, me]) => setUser(me.authenticated ? me.user : null))
      .catch(() => setGlobalMessage('サーバーに接続できません。バックエンドを起動してください。'))
      .finally(() => setAuthLoading(false))
  }, [])

  const logout = async () => {
    if (!window.confirm('ログアウトしますか？')) return
    try {
      await api<void>('/api/auth/logout', { method: 'POST' })
      setUser(null)
      setGlobalMessage('ログアウトしました。')
      navigate('/')
    } catch (error) {
      setGlobalMessage(getErrorMessage(error))
    }
  }

  const login = async (email: string, password: string) => {
    const response = await api<MeResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    setUser(response.user)
    setGlobalMessage('ログインしました。')
    navigate('/')
  }

  if (authLoading) return <main className="page"><p>読み込み中...</p></main>

  return (
    <>
      <Header user={user} navigate={navigate} logout={logout} />
      {globalMessage && <div className="message global-message">{globalMessage}</div>}
      <main className="page">
        {renderRoute(path, user, navigate, login, setGlobalMessage)}
      </main>
    </>
  )
}

function Header({ user, navigate, logout }: {
  user: User | null
  navigate: (to: string) => void
  logout: () => Promise<void>
}) {
  return (
    <header className="site-header">
      <button className="site-title" onClick={() => navigate('/')} type="button">ECサイト</button>
      <nav>
        <button onClick={() => navigate('/')} type="button">商品一覧</button>
        {user ? (
          <>
            <span>{user.name}さん</span>
            <button onClick={() => navigate('/cart')} type="button">カート</button>
            <button onClick={() => navigate('/history')} type="button">購入履歴</button>
            <button onClick={() => void logout()} type="button">ログアウト</button>
          </>
        ) : (
          <button onClick={() => navigate('/login')} type="button">ログイン</button>
        )}
      </nav>
    </header>
  )
}

function renderRoute(
  path: string,
  user: User | null,
  navigate: (to: string) => void,
  login: (email: string, password: string) => Promise<void>,
  setGlobalMessage: (message: string) => void,
) {
  if (path === '/login') return <LoginPage user={user} navigate={navigate} login={login} />
  if (path === '/cart') return <Protected user={user} navigate={navigate}><CartPage navigate={navigate} /></Protected>
  if (path === '/purchase/confirm') return <Protected user={user} navigate={navigate}><PurchaseConfirmPage navigate={navigate} setGlobalMessage={setGlobalMessage} /></Protected>
  if (path === '/purchase/complete') return <Protected user={user} navigate={navigate}><PurchaseCompletePage navigate={navigate} /></Protected>
  if (path === '/history') return <Protected user={user} navigate={navigate}><HistoryPage /></Protected>

  const productMatch = path.match(/^\/products\/(\d+)$/)
  if (productMatch) return <ProductDetailPage productId={Number(productMatch[1])} user={user} navigate={navigate} />
  return <HomePage navigate={navigate} />
}

function Protected({ user, navigate, children }: { user: User | null; navigate: (to: string) => void; children: ReactNode }) {
  useEffect(() => {
    if (!user) navigate('/login')
  }, [navigate, user])
  return user ? children : <p>ログイン画面へ移動しています...</p>
}

function HomePage({ navigate }: { navigate: (to: string) => void }) {
  const [allProducts, setAllProducts] = useState<Product[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [genre, setGenre] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api<Product[]>('/api/products')
      .then(setAllProducts)
      .catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [])

  useEffect(() => {
    const query = genre ? `?genre=${encodeURIComponent(genre)}` : ''
    api<Product[]>(`/api/products${query}`)
      .then(setProducts)
      .catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [genre])

  const genres = useMemo(() => [...new Set(allProducts.map((product) => product.genre))], [allProducts])

  return (
    <>
      <h1>商品一覧</h1>
      {error && <div className="message error">{error}</div>}
      <label>
        ジャンル：
        <select value={genre} onChange={(event) => setGenre(event.target.value)}>
          <option value="">すべて</option>
          {genres.map((item) => <option value={item} key={item}>{item}</option>)}
        </select>
      </label>
      {products.length === 0 ? <p>商品がありません。</p> : (
        <div className="product-list">
          {products.map((product) => <ProductCard product={product} navigate={navigate} key={product.productId} />)}
        </div>
      )}
    </>
  )
}

function ProductCard({ product, navigate }: { product: Product; navigate: (to: string) => void }) {
  return (
    <article className="product-card">
      <ProductImage product={product} />
      <div>
        <p>{product.genre}</p>
        <h2>{product.name}</h2>
        <p>{formatYen(product.price)}</p>
        <p>{product.stock > 0 ? `在庫：${product.stock}` : '在庫切れ'}</p>
        <button onClick={() => navigate(`/products/${product.productId}`)} type="button">詳細を見る</button>
      </div>
    </article>
  )
}

function ProductImage({ product }: { product: Product }) {
  return product.image
    ? <img className="product-image" src={product.image} alt="" />
    : <div className="product-image image-placeholder" aria-hidden="true">画像なし</div>
}

function LoginPage({ user, navigate, login }: { user: User | null; navigate: (to: string) => void; login: (email: string, password: string) => Promise<void> }) {
  const [email, setEmail] = useState('alice@example.com')
  const [password, setPassword] = useState('password')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (user) navigate('/')
  }, [navigate, user])

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
    } catch (reason) {
      setError(getErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="form-page">
      <h1>ログイン</h1>
      <form onSubmit={submit}>
        <label>メールアドレス<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
        <label>パスワード<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
        {error && <div className="message error">{error}</div>}
        <button disabled={submitting} type="submit">{submitting ? '処理中...' : 'ログイン'}</button>
      </form>
      <p>開発用：alice@example.com / password</p>
      <button onClick={() => navigate('/')} type="button">商品一覧へ戻る</button>
    </section>
  )
}

function ProductDetailPage({ productId, user, navigate }: { productId: number; user: User | null; navigate: (to: string) => void }) {
  const [product, setProduct] = useState<Product | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api<Product>(`/api/products/${productId}`)
      .then(setProduct)
      .catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [productId])

  if (error) return <div className="message error">{error}</div>
  if (!product) return <p>読み込み中...</p>

  const canAdd = Boolean(user) && product.stock > 0
  const addToCart = async () => {
    setMessage('')
    setError('')
    try {
      await api('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) })
      setMessage('カートに追加しました。')
    } catch (reason) {
      setError(getErrorMessage(reason))
    }
  }

  return (
    <section>
      <button onClick={() => navigate('/')} type="button">商品一覧へ戻る</button>
      <h1>{product.name}</h1>
      <ProductImage product={product} />
      <p>ジャンル：{product.genre}</p>
      <p>{product.description}</p>
      <p>価格：{formatYen(product.price)}</p>
      <p>{product.stock > 0 ? `在庫：${product.stock}` : '在庫切れ'}</p>
      <label>数量：<input min="1" type="number" value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} /></label>
      <div>
        <button disabled={!canAdd || quantity < 1} onClick={() => void addToCart()} type="button">カートに入れる</button>
        {!user && <p>カートに入れるにはログインしてください。</p>}
      </div>
      {message && <div className="message success">{message}</div>}
      {error && <div className="message error">{error}</div>}
    </section>
  )
}

function CartPage({ navigate }: { navigate: (to: string) => void }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api<Cart>('/api/cart').then(setCart).catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [])

  if (error) return <div className="message error">{error}</div>
  if (!cart) return <p>読み込み中...</p>

  return (
    <section>
      <h1>カート</h1>
      {cart.items.length === 0 ? <p>カートに商品がありません。</p> : (
        <>
          <CartTable items={cart.items} />
          <p className="total">合計：{formatYen(cart.totalAmount)}</p>
          <button onClick={() => navigate('/purchase/confirm')} type="button">購入する</button>
        </>
      )}
    </section>
  )
}

function CartTable({ items }: { items: CartItem[] }) {
  return (
    <table>
      <thead><tr><th>商品</th><th>数量</th><th>小計</th></tr></thead>
      <tbody>{items.map((item) => <tr key={item.cartId}><td>{item.product.name}</td><td>{item.quantity}</td><td>{formatYen(item.subtotal)}</td></tr>)}</tbody>
    </table>
  )
}

function PurchaseConfirmPage({ navigate, setGlobalMessage }: { navigate: (to: string) => void; setGlobalMessage: (message: string) => void }) {
  const [cart, setCart] = useState<Cart | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    api<Cart>('/api/cart').then(setCart).catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [])

  if (error) return <div className="message error">{error}</div>
  if (!cart) return <p>読み込み中...</p>

  const purchase = async () => {
    setSubmitting(true)
    setError('')
    try {
      await api<Purchase[]>('/api/purchases', { method: 'POST' })
      navigate('/purchase/complete')
    } catch (reason) {
      setError(getErrorMessage(reason))
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1>購入確認</h1>
      {cart.items.length === 0 ? <p>カートに商品がありません。</p> : (
        <>
          <CartTable items={cart.items} />
          <p className="total">合計：{formatYen(cart.totalAmount)}</p>
          {error && <div className="message error">{error}</div>}
          <button disabled={submitting} onClick={() => void purchase()} type="button">{submitting ? '処理中...' : '購入を確定する'}</button>
          <button disabled={submitting} onClick={() => { setGlobalMessage('購入をキャンセルしました。'); navigate('/cart') }} type="button">キャンセル</button>
        </>
      )}
    </section>
  )
}

function PurchaseCompletePage({ navigate }: { navigate: (to: string) => void }) {
  return (
    <section>
      <h1>購入完了</h1>
      <p>購入を受け付けました。</p>
      <button onClick={() => navigate('/')} type="button">商品一覧へ</button>
      <button onClick={() => navigate('/history')} type="button">購入履歴を見る</button>
    </section>
  )
}

function HistoryPage() {
  const [history, setHistory] = useState<Purchase[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api<Purchase[]>('/api/purchases').then(setHistory).catch((reason: unknown) => setError(getErrorMessage(reason)))
  }, [])

  if (error) return <div className="message error">{error}</div>
  if (!history) return <p>読み込み中...</p>
  return (
    <section>
      <h1>購入履歴</h1>
      {history.length === 0 ? <p>購入履歴がありません。</p> : (
        <table>
          <thead><tr><th>購入日時</th><th>商品</th><th>数量</th><th>購入価格</th><th>小計</th></tr></thead>
          <tbody>{history.map((item) => <tr key={item.purchaseId}><td>{new Date(item.purchasedAt).toLocaleString('ja-JP')}</td><td>{item.product.name}</td><td>{item.quantity}</td><td>{formatYen(item.price)}</td><td>{formatYen(item.subtotal)}</td></tr>)}</tbody>
        </table>
      )}
    </section>
  )
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '通信に失敗しました。'
}

export default App

# 🎨 DOMINation V2 - Sistema de Diseño

## Paleta de Colores Original

### Colores Principales

```css
--primary: #ff0e0e          /* Rojo Crimson - Color de acento */
--primary-dark: #ce0505     /* Rojo oscuro para hover */
--primary-light: #ff4444    /* Rojo claro para highlights */
```

### Colores Secundarios

```css
--secondary: #212629        /* Gris oscuro - Botones y cards */
--secondary-light: #3e4041  /* Gris claro para hover */
```

### Colores de Estado

```css
--success: #00a000          /* Verde para mensajes de éxito */
--danger: #ff0000           /* Rojo puro para errores */
--warning: #ff8800          /* Naranja para advertencias */
```

### Colores de Fondo

```css
--dark: #000000             /* Negro - Fondo principal */
--dark-alt: #1a1a1a         /* Negro suave para variaciones */
```

### Colores de Texto

```css
--light: #f5f5f5            /* Blanco roto para textos */
--gray: #808080             /* Gris medio */
--gray-dark: #4a4a4a        /* Gris oscuro */
--gray-light: #9e9999       /* Gris claro para texto secundario */
```

## Tipografía

### Fuentes

```css
/* Títulos y Navbar */
font-family: 'Bebas Neue', sans-serif;

/* Texto de Formularios */
font-family: 'Barlow Condensed', sans-serif;

/* Texto General */
font-family: 'Arial', sans-serif;
```

### Tamaños

```css
/* Page Title */
font-size: 3.5rem;
letter-spacing: 3px;

/* Section Headers */
font-size: 2.5rem;
letter-spacing: 2px;

/* Card Titles */
font-size: 2rem;
letter-spacing: 2px;

/* Navbar Links */
font-size: 1.5rem;
letter-spacing: 1px;

/* Buttons */
font-size: 1.3rem;
letter-spacing: 1.5px;

/* Body Text */
font-size: 1.1rem;
```

## Componentes

### Botones

#### Botón Primary (Gris)

```css
background: var(--secondary);       /* #212629 */
color: white;
border: 2px solid white;
border-radius: 50px;
padding: 0.85rem 2rem;
transition: all 0.3s ease;
```

**Hover:**
```css
background: var(--secondary-light); /* #3e4041 */
color: var(--primary);              /* #ff0e0e */
transform: scale(1.1);
```

#### Botón Success (Verde)

```css
background: var(--success);         /* #00a000 */
color: white;
border: 2px solid white;
border-radius: 50px;
```

**Hover:**
```css
background: var(--dark);
transform: scale(1.1);
```

#### Botón Danger (Rojo)

```css
background: var(--danger);          /* #ff0000 */
color: white;
border: 2px solid white;
border-radius: 50px;
```

### Cards

```css
background: var(--secondary);       /* #212629 */
border: 2px solid rgba(255, 255, 255, 0.1);
border-radius: 10px;
padding: 2rem;
box-shadow: 0 8px 32px rgba(206, 5, 5, 0.3);
```

**Hover:**
```css
background: var(--dark);
border-color: var(--primary);
transform: translateY(-8px) scale(1.02);
box-shadow: 0 12px 40px rgba(206, 5, 5, 0.5),
            0 0 30px rgba(255, 14, 14, 0.3);
```

### Badges

```css
background: rgba(206, 5, 5, 0.8);
color: white;
border: 2px solid white;
border-radius: 50px;
padding: 0.4rem 1.2rem;
font-size: 1.1rem;
letter-spacing: 1px;
```

### Formularios

#### Container

```css
background: var(--gray);            /* #808080 */
border: 2px solid rgba(255, 255, 255, 0.2);
border-radius: 10px;
padding: 2.5rem;
```

#### Header

```css
background: rgba(206, 5, 5, 0.8);
color: white;
padding: 0.75rem;
border-radius: 5px;
font-size: 2.5rem;
letter-spacing: 2px;
```

#### Inputs

```css
background: rgba(0, 0, 0, 0.8);
color: white;
border: none;
border-radius: 5px;
padding: 0.75rem 1rem;
font-size: 1.1rem;
```

**Focus:**
```css
background: rgba(0, 0, 0, 0.9);
box-shadow: 0 0 0 3px rgba(206, 5, 5, 0.5);
```

### Navbar

```css
background: rgba(33, 38, 41, 0.98);
backdrop-filter: blur(10px);
border-bottom: 3px solid var(--primary);
box-shadow: 0 4px 20px rgba(206, 5, 5, 0.4);
```

#### Logo/Title

```css
color: white;
text-shadow: 0 0 20px rgba(255, 14, 14, 0.7),
             0 0 40px rgba(255, 14, 14, 0.5);
```

#### Links

```css
color: white;
```

**Hover:**
```css
color: var(--primary);
text-shadow: 0 0 15px rgba(255, 14, 14, 0.8);
transform: translateY(-2px);
```

### Alerts

#### Error

```css
background: rgba(206, 5, 5, 0.8);
color: white;
border: 2px solid white;
border-radius: 12px;
```

#### Success

```css
background: var(--success);         /* #00a000 */
color: white;
border: 2px solid white;
```

#### Info

```css
background: var(--secondary);       /* #212629 */
color: white;
border: 2px solid rgba(255, 255, 255, 0.3);
```

## Efectos y Animaciones

### Text Shadow (Glow Effect)

```css
text-shadow: 0 0 20px rgba(255, 14, 14, 0.7),
             0 0 40px rgba(255, 14, 14, 0.5);
```

### Box Shadow (Card Glow)

```css
box-shadow: 0 8px 32px rgba(206, 5, 5, 0.3);

/* Hover */
box-shadow: 0 12px 40px rgba(206, 5, 5, 0.5),
            0 0 30px rgba(255, 14, 14, 0.3);
```

### Transitions

```css
transition: all 0.3s ease;
```

### Hover Transforms

```css
/* Buttons & Filters */
transform: scale(1.1);

/* Cards */
transform: translateY(-8px) scale(1.02);

/* Links */
transform: translateY(-2px);
```

## Uso de Colores por Contexto

### Fondos

- **Principal**: `var(--dark)` (#000000)
- **Cards**: `var(--secondary)` (#212629)
- **Formularios**: `var(--gray)` (#808080)
- **Inputs**: `rgba(0, 0, 0, 0.8)`

### Textos

- **Principal**: `white`
- **Secundario**: `var(--gray-light)` (#9e9999)
- **Títulos**: `white` con glow effect rojo
- **Highlight**: `var(--primary)` (#ff0e0e)

### Bordes

- **General**: `2px solid white`
- **Sutil**: `2px solid rgba(255, 255, 255, 0.1)`
- **Activo**: `border-color: var(--primary)`

### Botones

- **Primary**: Gris (#212629) → Hover: Gris claro con texto rojo
- **Success**: Verde (#00a000) → Hover: Negro
- **Danger**: Rojo (#ff0000) → Hover: Negro
- **Cancel**: Rojo con escala

## Iconografía

Los iconos se usan con moderación, solo para:
- User info: 👤
- Alertas: ⚠️ ✅ 💡
- Estados visuales en lugar de emojis decorativos

## Espaciado

```css
/* Padding Cards */
padding: 2rem;

/* Padding Buttons */
padding: 0.85rem 2rem;

/* Gaps */
gap: 1rem;         /* Filters */
gap: 1.5rem;       /* User info */
gap: 2rem;         /* Cards grid */

/* Margins */
margin-bottom: 2rem;    /* Sections */
margin-bottom: 2.5rem;  /* Form headers */
```

## Border Radius

```css
/* Buttons & Filters */
border-radius: 50px;

/* Cards & Forms */
border-radius: 10px;

/* Inputs & Badges Small */
border-radius: 5px;

/* Form Headers */
border-radius: 5px;
```

## Responsive Design

El diseño es responsive por defecto gracias a:

- Grid con `auto-fill, minmax(320px, 1fr)`
- Flexbox con `flex-wrap`
- Max-width en containers
- Unidades relativas (rem, %, vh)

## Accesibilidad

- Contraste alto: Negro sobre blanco/rojo
- Bordes visibles de 2px
- Focus states claros
- Font-size mínimo 1.1rem
- Letter-spacing para legibilidad

## Identidad de Marca

**DOMINation** se caracteriza por:

✅ Fondo negro sólido
✅ Rojo crimson como color de acento
✅ Efectos de glow/neón rojos
✅ Botones con bordes blancos redondeados
✅ Tipografía Bebas Neue para títulos
✅ Hover con scale(1.1)
✅ Cards en gris oscuro
✅ Alta legibilidad con texto blanco

**Inspiración**: Rock, música, energía, profesionalismo con actitud rebelde.



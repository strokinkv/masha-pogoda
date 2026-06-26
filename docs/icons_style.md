# Weather Icon Style

- Canvas: SVG `viewBox="0 0 64 64"`.
- Stroke: no contour strokes on main shapes; only weather details use colored strokes, such as rays, rain, snow, and fog lines.
- Palette: sun `#FFD43B`, rays `#FFB800`, moon `#FFF2A8`, day cloud `#8FD8FF`, night cloud `#6C7DFF`, rain `#009CFF`, snow `#00C2FF`, lightning `#FFE600`.
- Style: friendly cartoon shapes, bright fills, clear colored weather details, readable at small sizes.
- Runtime names must match `weatherCodeToIcon(...)`: `clear_day`, `clear_night`, `cloudy_day`, `cloudy_night`, `overcast`, `fog_day`, `fog_night`, `rain_light_day`, `rain_light_night`, `rain`, `snow_light_day`, `snow_light_night`, `snow`, `thunderstorm`, `sleet`.

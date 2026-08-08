\
\
\
\
\
\
\
\
   
import ezdxf
import os


def _outline_points_for_style(pts, style_key, piece="back"):
                                                    
    return [(round(x, 4), round(y, 4)) for x, y in pts]


def generate_dxf(pants_data, save_dir, filename):
                         
    doc = ezdxf.new("R2010")
    doc.header["$INSUNITS"] = 5
    doc.header["$MEASUREMENT"] = 1
    msp = doc.modelspace()

          
    doc.layers.add("轮廓", color=7)
    doc.layers.add("省道", color=7)
    doc.layers.add("辅助线", color=7)
    doc.layers.add("标注", color=7)

            
    doc.linetypes.add("DASHED2", pattern=[0.4, 0.2, -0.2])
    doc.linetypes.add("DASHDOT", pattern=[0.6, 0.4, -0.15, 0.0, -0.15])

    front_pts = [(float(x), float(y)) for x, y in pants_data["front"]]
    back_pts = [(float(x), float(y)) for x, y in pants_data["back"]]

    back_max_x = max(p[0] for p in back_pts)
    front_min_x = min(p[0] for p in front_pts)
                                 
    FRONT_OFFSET_X = back_max_x - front_min_x + 10.0
    front_shifted = [(x + FRONT_OFFSET_X, y) for x, y in front_pts]

    draw_mode = pants_data.get("draw_mode", "standard")
    style_key = pants_data.get("filename", "") or ""

                        
    _draw_contour(msp, back_pts, draw_mode, style_key, "back")

                        
    _draw_contour(msp, front_shifted, draw_mode, style_key, "front")

                                                               
    snap = draw_mode == "nvxiku"
    _draw_darts(msp, pants_data.get("back_dart", []), back_pts, 0, snap_waist=snap)
    _draw_darts(msp, pants_data.get("front_dart", []), front_pts, FRONT_OFFSET_X, snap_waist=snap)

                        
    wb_pts = [(float(x), float(y)) for x, y in pants_data["waistband"]]
    wb_bbox_pts = []
    if len(wb_pts) >= 3:
                  
        hem_y = min(min(p[1] for p in front_pts), min(p[1] for p in back_pts))
        wb_dy = hem_y - (max(p[1] for p in wb_pts)) - 8.0
        x_layout_min = min(p[0] for p in back_pts)
        x_layout_max = max(p[0] for p in front_shifted)
        layout_cx = (x_layout_min + x_layout_max) / 2
        wb_xs = [p[0] for p in wb_pts]
        wb_cx = (min(wb_xs) + max(wb_xs)) / 2
        wb_shift_x = layout_cx - wb_cx
        wb_raw = [(x + wb_shift_x, y + wb_dy) for x, y in wb_pts]
        wb_bbox_pts = list(wb_raw)
        wb_final = wb_raw + [wb_raw[0]]
        msp.add_lwpolyline(wb_final, dxfattribs={"layer": "轮廓"})

                               
    y_lines = pants_data.get("y_lines", {})

    bx_min = min(p[0] for p in back_pts) - 3
    bx_max = max(p[0] for p in back_pts) + 3
    fx_min = min(p[0] for p in front_shifted) - 3
    fx_max = max(p[0] for p in front_shifted) + 3

    for line_name, y_val in y_lines.items():
        msp.add_line((bx_min, y_val), (bx_max, y_val),
                      dxfattribs={"layer": "辅助线", "linetype": "DASHED2"})
        msp.add_line((fx_min, y_val), (fx_max, y_val),
                      dxfattribs={"layer": "辅助线", "linetype": "DASHED2"})

                                
    y_min = min(min(p[1] for p in back_pts), min(p[1] for p in front_pts))
    y_max = max(max(p[1] for p in back_pts), max(p[1] for p in front_pts))
                       
    msp.add_line((0, y_min - 2), (0, y_max + 3),
                  dxfattribs={"layer": "辅助线", "linetype": "DASHDOT"})
           
    msp.add_line((FRONT_OFFSET_X, y_min - 2), (FRONT_OFFSET_X, y_max + 3),
                  dxfattribs={"layer": "辅助线", "linetype": "DASHDOT"})

                          
    all_pts = back_pts + front_shifted
    if wb_bbox_pts:
        all_pts += wb_bbox_pts
    xs = [p[0] for p in all_pts]
    ys = [p[1] for p in all_pts]
    margin = 15.0
    xmin, ymin = min(xs) - margin, min(ys) - margin
    xmax, ymax = max(xs) + margin, max(ys) + margin
    cx, cy = (xmin + xmax) / 2, (ymin + ymax) / 2
    view_h = (ymax - ymin) * 1.1

    for var, val in [("$LIMMIN", (xmin, ymin)), ("$LIMMAX", (xmax, ymax)),
                     ("$EXTMIN", (xmin, ymin, 0)), ("$EXTMAX", (xmax, ymax, 0))]:
        try:
            doc.header[var] = val
        except Exception:
            pass

    try:
        vport = doc.viewports.get("*Active")
        if vport:
            for v in vport:
                v.dxf.center = (cx, cy)
                v.dxf.height = view_h
    except Exception:
        pass

          
    os.makedirs(save_dir, exist_ok=True)
    filepath = os.path.join(save_dir, f"{filename}.dxf")
    for i in range(20):
        try:
            doc.saveas(filepath)
            return filepath
        except PermissionError:
            filepath = os.path.join(save_dir, f"{filename}_{i + 1}.dxf")
    raise PermissionError("无法保存，请先关闭CAD中已打开的DXF文件")


def _waist_y(x, contour_pts):
                                             
    max_y = max(p[1] for p in contour_pts)
    band_lo = max_y - 3.0
    n = len(contour_pts)
    hits = []
    for i in range(n):
        x1, y1 = contour_pts[i]
        x2, y2 = contour_pts[(i + 1) % n]
        if max(y1, y2) < band_lo:
            continue
        if abs(x2 - x1) < 1e-6:
            if min(x1, x2) - 0.5 <= x <= max(x1, x2) + 0.5:
                hits.append(max(y1, y2))
        else:
            lo, hi = min(x1, x2), max(x1, x2)
            if lo - 0.5 <= x <= hi + 0.5:
                t = (x - x1) / (x2 - x1)
                t = max(0.0, min(1.0, t))
                hits.append(y1 + t * (y2 - y1))
    if hits:
        return max(hits)
    threshold = max_y * 0.95
    waist_idx = [i for i in range(n) if contour_pts[i][1] >= threshold]
    if len(waist_idx) < 2:
        return max_y
    for k in range(len(waist_idx) - 1):
        x1, y1 = contour_pts[waist_idx[k]]
        x2, y2 = contour_pts[waist_idx[k + 1]]
        lo, hi = min(x1, x2), max(x1, x2)
        if lo - 0.5 <= x <= hi + 0.5 and abs(x2 - x1) > 0.001:
            t = (x - x1) / (x2 - x1)
            t = max(0.0, min(1.0, t))
            return y1 + t * (y2 - y1)
    return max_y


def _draw_darts(msp, dart_list, contour_pts, offset_x, snap_waist=False):
                                                            
    for dart in dart_list:
        d = [(float(x), float(y)) for x, y in dart]
        if len(d) == 3:
            lx, ly = d[0]
            rx, ry = d[1]
            tip = (d[2][0] + offset_x, d[2][1])
            if snap_waist:
                top_l = (lx + offset_x, _waist_y(lx, contour_pts))
                top_r = (rx + offset_x, _waist_y(rx, contour_pts))
            else:
                top_l = (lx + offset_x, ly)
                top_r = (rx + offset_x, ry)
            msp.add_line(top_l, tip, dxfattribs={"layer": "省道"})
            msp.add_line(top_r, tip, dxfattribs={"layer": "省道"})


def _add_curve(msp, seg, degree=3):
                                                             
    if len(seg) >= 3:
        msp.add_spline(
            fit_points=[(x, y, 0) for x, y in seg],
            degree=degree,
            dxfattribs={"layer": "轮廓"},
        )
    elif len(seg) == 2:
        msp.add_line(seg[0], seg[1], dxfattribs={"layer": "轮廓"})


def _draw_auto_curve(msp, pts):
                                          
    if len(pts) <= 1:
        return
    if len(pts) == 2:
        msp.add_line(pts[0], pts[1], dxfattribs={"layer": "轮廓"})
        return
    segments = [[pts[0]]]
    for i in range(1, len(pts)):
        segments[-1].append(pts[i])
        if i < len(pts) - 1:
            dx_prev = pts[i][0] - pts[i - 1][0]
            dx_next = pts[i + 1][0] - pts[i][0]
            if dx_prev * dx_next < 0:
                segments.append([pts[i]])
    for seg in segments:
        _add_curve(msp, seg)


def _waist_threshold_y(pts):
                                               
    y_max = max(p[1] for p in pts)
    y_min = min(p[1] for p in pts)
    span = y_max - y_min + 1e-9
    band = min(10.0, max(5.0, 0.11 * span))
    return y_max - band


def _spline_degree_for_style(style_key):
                                       
    k = (style_key or "").strip().lower()
    return 2 if k == "qianbiku" else 3


def _draw_contour_nvxiku_style(msp, pts, style_key=""):
\
\
\
       
    n = len(pts)
    thr = _waist_threshold_y(pts)
    waist_indices = [i for i in range(n) if pts[i][1] >= thr]
    if len(waist_indices) < 2:
        outline = [(round(x, 4), round(y, 4)) for x, y in pts]
        msp.add_lwpolyline(
            [(float(x), float(y)) for x, y in outline],
            close=True,
            dxfattribs={"layer": "轮廓"},
        )
        return

    idx_wr = waist_indices[0]
    idx_wl = waist_indices[-1]
    inner_indices = list(range(idx_wl, n))
    crotch_idx = min(inner_indices, key=lambda i: pts[i][0])
    deg = _spline_degree_for_style(style_key)

    _add_curve(msp, pts[: idx_wr + 1], degree=deg)
    for i in range(idx_wr, idx_wl):
        msp.add_line(pts[i], pts[i + 1], dxfattribs={"layer": "轮廓"})
    _add_curve(msp, pts[idx_wl : crotch_idx + 1], degree=deg)
    _add_curve(msp, pts[crotch_idx:], degree=deg)
    msp.add_line(pts[-1], pts[0], dxfattribs={"layer": "轮廓"})


def _draw_contour(msp, pts, draw_mode="standard", style_key="", piece="back"):
\
\
\
\
       
    n = len(pts)
    if n < 3:
        return

    if draw_mode == "polyline":
        outline = _outline_points_for_style(pts, style_key, piece)
        msp.add_lwpolyline(
            [(float(x), float(y)) for x, y in outline],
            close=True,
            dxfattribs={"layer": "轮廓"},
        )
        return

    _draw_contour_nvxiku_style(msp, pts, style_key)


def format_coordinates_text(pants_data, pants_type_name):
                      
    lines = []
    meta = pants_data["meta"]
    lines.append(f"【{pants_type_name}】{pants_data['name']}")
    lines.append(f"成品腰围: {meta['成品腰围']}cm  成品臀围: {meta['成品臀围']}cm")
    lines.append(f"总裤长: {meta['总裤长']}cm  立裆: {meta['立裆']}cm")
    lines.append("")

    y_lines = pants_data.get("y_lines", {})
    y_str = "  ".join([f"{k}={v}" for k, v in y_lines.items()])
    lines.append(f"纵向基准Y值: {y_str}")
    lines.append("")

    lines.append("--- 前片坐标 (A系列) ---")
    for i, (x, y) in enumerate(pants_data["front"], 1):
        lines.append(f"  A{i:>2d}  ({x:>8.2f}, {y:>8.2f})")

    lines.append("")
    lines.append("--- 后片坐标 (B系列) ---")
    for i, (x, y) in enumerate(pants_data["back"], 1):
        lines.append(f"  B{i:>2d}  ({x:>8.2f}, {y:>8.2f})")

    for label, dart_list in [("前片省道", pants_data.get("front_dart", [])),
                              ("后片省道", pants_data.get("back_dart", []))]:
        if dart_list:
            lines.append("")
            lines.append(f"--- {label} ---")
            for di, dart in enumerate(dart_list, 1):
                names = ["省左点", "省右点", "省尖点"]
                for name, (x, y) in zip(names, dart):
                    lines.append(f"  省{di} {name}  ({x:>8.2f}, {y:>8.2f})")

    lines.append("")
    lines.append("--- 腰头坐标 (C系列) ---")
    for i, (x, y) in enumerate(pants_data["waistband"]):
        lines.append(f"  C{i}  ({x:>8.2f}, {y:>8.2f})")

    return "\n".join(lines)

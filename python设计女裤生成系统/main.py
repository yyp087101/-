\
\
\
   
import tkinter as tk
from tkinter import ttk, messagebox, filedialog, scrolledtext
import os
import subprocess
import sys

from pants_data import UI_PANTS_LIST, PANTS_DB, get_pants_data
from dxf_generator import generate_dxf, format_coordinates_text


class PantsGeneratorApp:
    def __init__(self, root):
        self.root = root
        self.root.title("女裤样板生成系统")
        self.root.geometry("1030x620")
        self.root.resizable(False, False)

        self.selected_pants = tk.StringVar(value="女西裤")
        self.save_dir = tk.StringVar(value=os.path.join(os.path.expanduser("~"), "Desktop", "dxf"))
        self.last_dxf_path = None

        self._build_ui()

    def _build_ui(self):
                       
        type_frame = tk.LabelFrame(self.root, text="裤型选择", padx=10, pady=5)
        type_frame.place(x=10, y=5, width=1010, height=75)

        row1_types = UI_PANTS_LIST[:5]
        row2_types = UI_PANTS_LIST[5:]

        for i, name in enumerate(row1_types):
            tk.Radiobutton(
                type_frame, text=name, variable=self.selected_pants,
                value=name, font=("SimSun", 10)
            ).grid(row=0, column=i, padx=15, pady=2, sticky="w")

        for i, name in enumerate(row2_types):
            tk.Radiobutton(
                type_frame, text=name, variable=self.selected_pants,
                value=name, font=("SimSun", 10)
            ).grid(row=1, column=i, padx=15, pady=2, sticky="w")

                       
        input_frame = tk.LabelFrame(self.root, text="输入尺寸", padx=10, pady=10)
        input_frame.place(x=10, y=90, width=160, height=420)

        labels = ["坐高", "腰高", "腰围", "臀围"]
        self.size_entries = {}

        for i, label in enumerate(labels):
            tk.Label(input_frame, text=label, font=("SimSun", 11)).grid(row=i, column=0, pady=8, sticky="e")
            entry = tk.Entry(input_frame, width=8, font=("SimSun", 11))
            entry.grid(row=i, column=1, padx=5, pady=8)
            self.size_entries[label] = entry

        self._fill_defaults()
        self.selected_pants.trace_add("write", lambda *_: self._fill_defaults())

                       
        btn_frame = tk.LabelFrame(self.root, text="控制按钮", padx=10, pady=10)
        btn_frame.place(x=180, y=90, width=180, height=420)

        btn_style = {"width": 16, "height": 2, "font": ("SimSun", 10)}

        tk.Button(btn_frame, text="计算坐标", command=self._calc_coords, **btn_style).pack(pady=6)
        tk.Button(btn_frame, text="绘制并保存DXF", command=self._save_dxf, **btn_style).pack(pady=6)
        tk.Button(btn_frame, text="在CAD中打开", command=self._open_autocad, **btn_style).pack(pady=6)
        tk.Button(btn_frame, text="打开文件夹", command=self._open_folder, **btn_style).pack(pady=6)
        tk.Button(btn_frame, text="选择保存目录", command=self._choose_dir, **btn_style).pack(pady=6)

                         
        result_frame = tk.LabelFrame(self.root, text="计算坐标结果", padx=5, pady=5)
        result_frame.place(x=370, y=90, width=650, height=420)

        self.result_text = scrolledtext.ScrolledText(
            result_frame, wrap=tk.WORD, font=("Consolas", 9), state=tk.DISABLED
        )
        self.result_text.pack(fill=tk.BOTH, expand=True)

                       
        status_frame = tk.Frame(self.root)
        status_frame.place(x=10, y=520, width=1010, height=30)

        self.status_label = tk.Label(
            status_frame, textvariable=self.save_dir, anchor="w",
            relief=tk.SUNKEN, font=("SimSun", 9)
        )
        self.status_label.pack(side=tk.LEFT, fill=tk.X, expand=True)

        tk.Button(
            status_frame, text="选择路径", command=self._choose_dir, font=("SimSun", 9)
        ).pack(side=tk.RIGHT)

    def _fill_defaults(self):
                                 
        pants_type = self.selected_pants.get()
        if pants_type not in PANTS_DB:
            return
        std = PANTS_DB[pants_type]["std_input"]
        for key, entry in self.size_entries.items():
            entry.delete(0, tk.END)
            v = std[key]
            entry.insert(0, str(int(v)) if v == int(v) else str(v))

    def _get_current_data(self):
        pants_type = self.selected_pants.get()
        try:
            zuo_gao = float(self.size_entries["坐高"].get())
            yao_gao = float(self.size_entries["腰高"].get())
            yao_wei = float(self.size_entries["腰围"].get())
            tun_wei = float(self.size_entries["臀围"].get())
        except ValueError:
            messagebox.showerror("错误", "请输入正确的数字尺寸")
            return None, None
        try:
            data = get_pants_data(pants_type, zuo_gao, yao_gao, yao_wei, tun_wei)
        except Exception as e:
            messagebox.showerror("错误", f"裤型「{pants_type}」计算失败:\n{e}")
            return None, None
        return pants_type, data

    def _calc_coords(self):
        pants_type, data = self._get_current_data()
        if data is None:
            return

        text = format_coordinates_text(data, pants_type)

        self.result_text.config(state=tk.NORMAL)
        self.result_text.delete("1.0", tk.END)
        self.result_text.insert(tk.END, text)
        self.result_text.config(state=tk.DISABLED)

    def _save_dxf(self):
        pants_type, data = self._get_current_data()
        if data is None:
            return

        save_dir = self.save_dir.get()
        filename = data.get("filename", "pants")

        try:
            filepath = generate_dxf(data, save_dir, filename)
            self.last_dxf_path = filepath
            self._calc_coords()
            messagebox.showinfo("成功", f"DXF文件已保存:\n{filepath}")
        except Exception as e:
            messagebox.showerror("生成失败", f"DXF生成错误:\n{str(e)}")

    def _open_autocad(self):
        if not self.last_dxf_path or not os.path.exists(self.last_dxf_path):
            messagebox.showwarning("提示", "请先生成DXF文件")
            return
        dxf_path = os.path.normpath(self.last_dxf_path)
        try:
            subprocess.Popen(f'cmd /c start "" "{dxf_path}"', shell=True)
        except Exception:
            try:
                os.startfile(dxf_path)
            except Exception as e:
                messagebox.showerror("打开失败", f"无法打开:\n{e}\n\n请手动双击:\n{dxf_path}")
                return
        messagebox.showinfo(
            "提示",
            "DXF文件已发送到关联CAD软件打开。\n\n"
            "如果打开后看不到图形，请在CAD命令行输入:\n"
            "  ZOOM  回车 → E  回车\n"
            "即可缩放到全图。"
        )

    def _open_folder(self):
        save_dir = self.save_dir.get()
        if not os.path.exists(save_dir):
            os.makedirs(save_dir, exist_ok=True)
        os.startfile(save_dir)

    def _choose_dir(self):
        dir_path = filedialog.askdirectory(initialdir=self.save_dir.get(), title="选择DXF保存目录")
        if dir_path:
            self.save_dir.set(dir_path)


def main():
    root = tk.Tk()
    app = PantsGeneratorApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()

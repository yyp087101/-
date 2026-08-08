// 通用工具函数

// Toast提示
function showToast(msg, type) {
    type = type || 'success';
    var container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    var toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(function() {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s';
        setTimeout(function() { toast.remove(); }, 300);
    }, 3000);
}

// AJAX POST请求
function ajaxPost(url, data, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', url, true);
    if (typeof data === 'string') {
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        xhr.send(data);
    } else if (data instanceof FormData) {
        xhr.send(data);
    } else {
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        var params = [];
        for (var key in data) {
            if (data[key] !== null && data[key] !== undefined) {
                params.push(encodeURIComponent(key) + '=' + encodeURIComponent(data[key]));
            }
        }
        xhr.send(params.join('&'));
    }
    xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                var res = JSON.parse(xhr.responseText);
                callback(res);
            } else {
                showToast('请求失败', 'error');
            }
        }
    };
}

// 确认弹窗
function confirmAction(msg, callback) {
    if (confirm(msg)) {
        callback();
    }
}

// 打开模态框
function openModal(id) {
    document.getElementById(id).classList.add('show');
}

// 关闭模态框
function closeModal(id) {
    document.getElementById(id).classList.remove('show');
}

// 序列化表单
function serializeForm(formId) {
    var form = document.getElementById(formId);
    var elements = form.elements;
    var data = {};
    for (var i = 0; i < elements.length; i++) {
        var el = elements[i];
        if (el.name && el.type !== 'button' && el.type !== 'submit') {
            data[el.name] = el.value;
        }
    }
    return data;
}

// 文件上传
function uploadFile(inputId, callback) {
    var input = document.getElementById(inputId);
    var file = input.files[0];
    if (!file) { showToast('请选择文件', 'error'); return; }
    var formData = new FormData();
    formData.append('file', file);
    ajaxPost('/file/upload', formData, function(res) {
        if (res.code === 200) {
            callback(res.data);
        } else {
            showToast(res.msg, 'error');
        }
    });
}

// 图片预览
function previewImage(inputId, previewId) {
    var input = document.getElementById(inputId);
    var preview = document.getElementById(previewId);
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            preview.style.display = 'block';
        };
        reader.readAsDataURL(input.files[0]);
    }
}

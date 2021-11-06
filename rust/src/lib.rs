use std::borrow::{Borrow, BorrowMut};
use std::ffi::{CString, IntoStringError};
use std::os::raw::c_char;

#[no_mangle]
pub extern "system" fn print_from_rust() {
    println!("--- RUST execute something  from rust")
}


#[no_mangle]
pub extern "system" fn get_string_in_rust(raw: *mut c_char) {
    unsafe {
        let c_string = CString::from_raw(raw);
        match c_string.into_string() {
            Ok(java_str) => {
                println!("--- RUST get from java {}", java_str)
            }
            Err(err) => {
                panic!(err)
            }
        };
    }
}

#[no_mangle]
pub extern "system" fn return_string_from_rust(mut raw: *mut c_char) -> *mut c_char {
    unsafe {
        let mut c_string = CString::from_raw(raw);
        return match c_string.into_string() {
            Ok(java_str) => {
                println!("--- RUST got string from java  {:?}", java_str);
                let str_bytes = b"string from rust".to_vec();
                c_string = CString::from_vec_unchecked(str_bytes);
                println!("--- RUST prepare string {:?}", c_string);
                c_string.into_raw()
            }
            Err(err) => {
                panic!(err)
            }
        };
    }
}

#[no_mangle]
pub extern "system" fn call_back(cb: extern fn(raw: *mut c_char) -> *mut c_char) -> *mut c_char {
    unsafe {
        let str_bytes = b"callback from rust".to_vec();
        let c_string = CString::from_vec_unchecked(str_bytes);
        cb( c_string.into_raw())
    }
}

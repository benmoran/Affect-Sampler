''' Cookie-handlin' mix-in helper; inspired by WebOb.

This module offers a cookie-handling mixin class meant to be used with Google
App Engine; this class can in fact be mixed into any class that shares the
following features with webapp.RequestHandler subclasses:
  - a self.request.cookies object with a get(key, defaultvalue) method
  - a self.response.headers object offering:
        - methods add_header(header, value) and getall(header)
        - the ability to 'del self.response.headers[header]'

The mixin class supplies methods to get_, set_, delete_ and unset_ a cookie
(each method's name ends with _cookie;-).
'''
# Copyright (C) 2008 aleaxit@gmail.com
# licensed under CC-by license, http://creativecommons.org/licenses/by/3.0/

import Cookie
import datetime
import time
from Cookie import BaseCookie


def _serialize_cookie_date(dt):
  dt = dt.timetuple()
  return time.strftime('"%a, %d-%b-%Y %H:%M:%S GMT"', dt.timetuple())


class CookieMixin(object):

  def get_cookie(self, key, default_value=None):
    """ Gets a cookie from the request object:

    Args:
      key: string that's the cookie's name (mandatory)
      default_value: default value if name's absent (default: None)
    Returns:
      a string (the cookie's value) or the default value if the cookie's absent
    """
    return self.request.cookies.get(key, default_value)

  def set_cookie(self, key, value='', max_age=None,
       path='/', domain=None, secure=None, httponly=False,
       version=None, comment=None, expires=None):
    """ Set (add) a cookie to the response object.

    Args:
      key: string that is the cookie's name (mandatory)
      value: string (or Unicode) that is the cookie's value (default '')
    and many optional ones to set the cookie's properties (pass BY NAME only!):
      max_age (or datetime.timedelta or a number of seconds)
      expires (string, datetime.timedelta, or datetime.datetime)
    [if you pass max_age and not expires, expires is computed from max_age]
      path, domain, secure, httponly, version, comment (typically strings)
    Side effects:
      adds to self.response.headers an appropriate Set-Cookie header.
    """
    if isinstance(value, unicode):
      value = '"%s"' % value.encode('utf8')
    cookies = Cookie.BaseCookie()
    cookies[key] = value
    if isinstance(max_age, datetime.timedelta):
      max_age = datetime.timedelta.seconds + datetime.timedelta.days*24*60*60
    if max_age is not None and expires is None:
      expires = (datetime.datetime.utcnow() + 
                 datetime.timedelta(seconds=max_age))
    if isinstance(expires, datetime.timedelta):
      expires = datetime.datetime.utcnow() + expires
    if isinstance(expires, datetime.datetime):
      expires = '"'+_serialize_cookie_date(expires)+'"'
    for var_name, var_value in [
        ('max_age', max_age),
        ('path', path),
        ('domain', domain),
        ('secure', secure),
        ('HttpOnly', httponly),
        ('version', version),
        ('comment', comment),
        ('expires', expires),
        ]:
      if var_value is not None and var_value is not False:
        cookies[key][var_name.replace('_', '-')] = str(var_value)
    header_value = cookies[key].output(header='').lstrip()
    self.response.headers.add_header('Set-Cookie', header_value)

  def delete_cookie(self, key, path='/', domain=None):
    """ Delete a cookie from the client.
    
    Path and domain must match how the cookie was originally set.  This method
    sets the cookie to the empty string, and max_age=0 so that it should 
    expire immediately (a negative expires should also help with that)

    Args: 
      key: string that is the cookie's name (mandatory)
      path, domain: optional strings, must match the original settings
    Side effects:
      adds to self.response.headers an appropriate Set-Cookie header.
    """
    self.set_cookie(key, '', path=path, domain=domain,
                    max_age=0, expires=datetime.timedelta(days=-5))

  def unset_cookie(self, key):
    """ Unset a cookie with the given name (remove from the response).
    
    If there are multiple cookies (e.g., two cookies with the same name and
    different paths or domains), all such cookies will be deleted.
      
    Args:
      key: string that is the cookie's name (mandatory)
    Side effects:
      delete from self.response.headers all cookies with that name
    Raises:
      KeyError if the response had no such cookies (or, none at all)
    """
    existing = self.response.headers.getall('Set-Cookie')
    if not existing: raise KeyError("No cookies at all had been set")
    # remove all set-cookie headers, then put back those (if any) that
    # should not be removed
    del self.response.headers['Set-Cookie']
    found = False
    for header in existing:
      cookies = BaseCookie()
      cookies.load(header)
      if key in cookies:
        found = True
        del cookies[key]
        header = cookies.output(header='').lstrip()
      if header:
        self.response.headers.add_header('Set-Cookie', header)
    if not found: raise KeyError("No cookie had been set with name %r" % key)


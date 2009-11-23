import cgi
import doctest
import logging
import os
import re

logger = logging.getLogger()
logger.setLevel(getattr(logging, os.environ.get('LOGLEVEL', 'WARNING')))

class UrlParser(object):
  """ Parse a URL path and perform appropriate an callback on regex-matching.

      Instantiate h with a prefix (to be matched, but ignored if it matches),
        followed by as many (regex, callback) pairs as needed.
      Then, call h.process(path): if the path matches the prefix, then
        each regex is tried *IN ORDER* on the rest of the path, and,
        upon the first match if any, the corresponding callback gets called
        (and its results returned).
        If the prefix does not match, or none of the regexes does, then
        method call h.process(path) returns None.
      The callback is passed *NAMED* arguments (only!) corresponding to
        the positional groups matched in the prefix, augmented or overridden
        by those matched in the specific regex that matched after that.
      So for example:
      >>> def show(**k): print sorted(k.items())
      >>> h = UrlParser(r'/(?P<foo>\w+)/',
      ...                (r'(?P<bar>\d+)', show),
      ...                (r'(?P<foo>[^/]*)', show),
      ...                )
      >>> h.process('/zipzop/23/whatever')
      [('bar', '23'), ('foo', 'zipzop')]
      >>> h.process('/zipzop/whoo/whatever')
      [('foo', 'whoo')]

      You can also change the prefix by passing a prefix to .process(...) [the
      new prefix-to-ignore is then remembered in lieu of the previous one].

      >>> h.prefix.pattern
      '/(?P<foo>\\\\w+)/'
      >>> h.process('/zipzop/whoo/whatever', prefix='/')
      [('foo', 'zipzop')]
      >>> h.process('/zipzop/whoo/whatever')
      [('foo', 'zipzop')]
      >>> h.prefix.pattern
      '/'

      The h.prefix attribute is exposed, and it's a RE object.
  """

  def __init__(self, prefix, *args):
    """ Takes a prefix to be ignored and 0+ (regex, callback) pair args.

    Args:
      prefix: a string regex pattern
      args: 0+ pairs (regex_pattern, callback) [each a string + a callable]
    """
    self.prefix = re.compile(prefix or '')
    logging.debug('prefix: %r', prefix)
    self.callbacks = []
    for pattern, callback in args:
      logging.debug('%r -> %r', pattern, callback)
      self.callbacks.append((re.compile(pattern), callback))

  def process(self, path, prefix=None):
    """ Match the path to one of the regexs and call the appropriate callback.

    Args:
      path: a string URL (complete path) to parse
      prefix: if not None, a RE pattern string to change self.prefix from now on
    Returns:
      the result of the appropriate callback, or None if no match
    """
    if prefix is not None and prefix != self.prefix.pattern:
      self.prefix = re.compile(prefix)
    prefix_mo = self.prefix.match(path)
    if prefix_mo is None:
      logging.debug('No prefix match for %r (%r)', path, self.prefix)
      return None
    pathrest = path[prefix_mo.end():]
    logging.debug('Matching %r...', pathrest)
    for regex, callback in self.callbacks:
      mo = regex.match(pathrest)
      if mo:
        logging.debug('Matched %r, calling %r', regex, callback)
        named_args = prefix_mo.groupdict()
        named_args.update(mo.groupdict())
        return callback(**named_args)
    logging.debug('No match for %r', pathrest)
    return None


class RestUrlParser(UrlParser):
  """ Specifically dispatches on the REs associated with REST-shaped URLs.

  Note that h.process only takes an URL *path*, NOT the rest of the URL (no
  protocol, no host, no query).

  >>> h = RestUrlParser('')
  >>> h.process('/$foobar')
  ('special', '$foobar')
  >>> h.process('/foobar')
  ('model', 'foobar')
  >>> h.process('/$foobar/zak/')
  ('special_method', '$foobar', 'zak')
  >>> h.process('/foobar/zak/')
  ('model_method', 'foobar', 'zak')
  >>> h.process('/foobar/23/')
  ('model_strid', 'foobar', '23')
  >>> h.process('/foobar/23/blop')
  ('model_strid_method', 'foobar', '23', 'blop')
  >>> h.process('')
  >>> h.process('////////')
  >>>
  """

  @staticmethod
  def _doprefix(prefix):
    if prefix is None: return None
    prefix = prefix.strip('/')
    if prefix: return '/%s/' % prefix
    else: return '/'

  def process(self, path, prefix=None):
    return UrlParser.process(self, path, self._doprefix(prefix))

  def __init__(self, prefix=None, **overrides):
    """ Set the prefix-to-ignore, optionally override methods.

    Args:
      prefix: a string regex pattern (or None, default)
      overrides: 0+ named arguments; values are callables to override the
        methods RestUrlParser provides (which just return tuples of strings),
        and each such callable must be signature-compatible with the
        corresponding named method.  The methods & signaturs are:
          do_special(special)
          do_model(model)
          do_special_method(special, method)
          do_model_method(model, method)
          do_model_strid(model, strid)
          do_model_strid_method(model, strid, method)

        The *names* (not necessarily the *order*) of the arguments matter.

        The values of all arguments are strings (the substrings of the
          incoming path that match the respective items of the REST URL):
            strid is always 1+ digits; special is '$' + a valid identifier;
            model and method are identifiers.
    """
    # let each method be overridden (in the instance) by caller at ctor-time
    self.__dict__.update(overrides)

    # prefix must always absorb leading and trailing /
    prefix = self._doprefix(prefix)

    # build URL regexes with corresponding names
    urls = []
    def addurl(name, regex): urls.append((regex, getattr(self, 'do_'+name)))

    sr_method = r'/(?P<method>\w+)'
    sr_strid = r'/(?P<strid>\d+)'

    # special_method must be before special (ie. special_method > special)
    re_special = r'(?P<special>\$\w+)/?'
    re_special_method = re_special + sr_method
    addurl('special_method', re_special_method)
    addurl('special', re_special)

    # model_strid_method > model_strid > model_method > model
    re_model = r'(?P<model>\w+)/?'
    re_model_method = re_model + sr_method
    re_model_strid = re_model + sr_strid
    re_model_strid_method = re_model_strid + sr_method
    addurl('model_strid_method', re_model_strid_method)
    addurl('model_strid', re_model_strid)
    addurl('model_method', re_model_method)
    addurl('model', re_model)

    UrlParser.__init__(self, prefix, *urls)

  def do_special(self, special):
    return 'special', special
  def do_model(self, model):
    return 'model', model
  def do_special_method(self, special, method):
    return 'special_method', special, method
  def do_model_method(self, model, method):
    return 'model_method', model, method
  def do_model_strid(self, model, strid):
    return 'model_strid', model, strid
  def do_model_strid_method(self, model, strid, method):
    return 'model_strid_method', model, strid, method


def _test():
    import doctest
    numfailures, numtests = doctest.testmod()
    if numfailures == 0:
      print '%d tests passed successfully' % numtests
    # if there are any failures, doctest does its own reporting!-)

if __name__ == "__main__":
    _test()

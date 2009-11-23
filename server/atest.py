""" A very simple "smoke test" for gae-json-rest toy app. """
import sys
import simplejson
import testutil

class TemplateTest(object):
  def emit(self, s):
    if self.verbose: print s
  
  def __call__(self, tester, verbose):
    self.verbose = verbose

    # what models do we have? shd be Doctor and Pager
    self.emit('Getting names for Models:')
    modelnames = tester.request_and_show('GET', '/')
    try:
      assert set(modelnames) == set(('Doctor', 'Pager'))
    except:
      print 'modelnames is', set(modelnames)
      raise

    # do we know any Doctors?
    self.emit('IDs of Doctors before any operations:')
    doctorids = tester.request_and_show('GET', '/Doctor/')
    # get the highest-known Doctor ID, if any, to ensure a unique number
    if doctorids:
      unique = max(int(obj['id']) for obj in doctorids) + 1
    else:
      unique = 1
    # now, we want to delete about half the doctors we know
    num_doctors = len(doctorids)
    deletions = 0
    for i in range(0, num_doctors, 2):
      strid = doctorids[i]['id']
      tester.silent_request('DELETE', '/Doctor/%s' % strid)
      deletions += 1
    self.emit('IDs of Doctors after some deletions:')
    doctorids = tester.silent_request('GET', '/Doctor/')
    self.emit(doctorids)
    if len(doctorids) != num_doctors - deletions:
      print 'Had %d doctors, deleted %d, should have %d but have %d' % (
          num_doctors, deletions, num_doctors-deletions, len(doctorids))
      sys.exit(1)
    num_doctors = len(doctorids)

    # form name based on unique number
    docname = 'Dr. John %s' % unique
    # make entity with that name
    post_body = testutil.body(name=docname)
    post_result = tester.request_and_show('POST', '/Doctor/', post_body)
    new_doctor_id = post_result['id']
    new_doctor_path = '/Doctor/%s' % new_doctor_id
    self.emit('Created %r' % new_doctor_path)
    # show new doctor just created
    self.emit('New Doctor just created:')
    new_doctor = tester.request_and_show('GET', new_doctor_path)
    if new_doctor['name'] != docname:
      print 'New doctor name should be %r, is %r instead after POST' % (
          docname, new_doctor['name'])
      sys.exit(1)
    # show IDs after the POST
    self.emit('IDs of Doctors after POST:')
    doctorids = tester.request_and_show('GET', '/Doctor/')
    if len(doctorids) != num_doctors + 1:
      print 'Had %d doctors, created %d, should have %d but have %d' % (
          num_doctors, 1, num_doctors+1, len(doctorids))
      sys.exit(1)
    num_doctors = len(doctorids)

    # Now change the name of the doctor
    docname = '%s changed' % docname
    put_body = testutil.body(name=docname)
    put_result = tester.request_and_show('PUT', new_doctor_path, put_body)
    # show new doctor just changed
    self.emit('New Doctor just changed:')
    new_doctor = tester.request_and_show('GET', new_doctor_path)
    if new_doctor['name'] != docname:
      print 'New doctor name should be %r, is %r instead after PUT' % (
          docname, new_doctor['name'])
      sys.exit(1)
    self.emit('IDs of Doctors after PUT:')
    doctorids = tester.request_and_show('GET', '/Doctor/')
    if len(doctorids) != num_doctors:
      print 'Had %d doctors, put %d, should have %d but have %d' % (
          num_doctors, 1, num_doctors, len(doctorids))
      sys.exit(1)

    # check idempotence of PUT
    self.emit('Check PUT idempotence')
    tester.request_and_show('PUT', new_doctor_path, put_body)
    # show new doctor just not-changed
    self.emit('New Doctor just not-changed:')
    new_doctor = tester.request_and_show('GET', new_doctor_path)
    if new_doctor['name'] != docname:
      print 'New doctor name should be %r, is %r instead after 2nd PUT' % (
          docname, new_doctor['name'])
      sys.exit(1)
    
    self.emit('IDs of Doctors after second PUT:')
    doctorids = tester.request_and_show('GET', '/Doctor/')
    if len(doctorids) != num_doctors:
      print 'Had %d doctors, put %d again, should have %d but have %d' % (
          num_doctors, 1, num_doctors, len(doctorids))
      sys.exit(1)
    
    # testing cookie functionality
    # each call to test_cookie should return an incremented value of
    # cookie named secret_key
    self.emit('Testing cookie functionality')
    a = int(tester.get_cookies().get('counter'))
    b = int(tester.get_cookies().get('counter'))
    c = int(tester.get_cookies().get('counter'))
    if a+1 != b or b+1 != c:
      print 'a, b and c should have been 0, 1 and 2 respectively.'
      print 'Got a=%d, b=%d and c=%d' % (a, b, c)
      sys.exit(1)


if __name__ == '__main__':
  test = TemplateTest()
  t = testutil.Tester(test)
  t.execute()

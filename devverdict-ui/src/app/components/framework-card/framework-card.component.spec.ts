import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FrameworkCardComponent } from './framework-card.component';
import { Framework } from '../../models/framework.model';

describe('FrameworkCardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FrameworkCardComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(FrameworkCardComponent);
    fixture.componentRef.setInput('framework', {
      id: 1,
      name: 'Angular',
      type: 'Framework',
      description: 'A platform for building mobile and desktop web applications.',
      averageRating: 4.2
    } as Framework);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display framework name', () => {
    const fixture = TestBed.createComponent(FrameworkCardComponent);
    fixture.componentRef.setInput('framework', {
      id: 1,
      name: 'Angular',
      type: 'Framework',
      description: 'A platform for building mobile and desktop web applications.',
      averageRating: 4.2
    } as Framework);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Angular');
  });
});
